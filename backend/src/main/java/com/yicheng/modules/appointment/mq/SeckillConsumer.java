package com.yicheng.modules.appointment.mq;


import com.yicheng.common.RedisConstants;
import com.yicheng.config.RabbitConfig;
import com.yicheng.modules.appointment.mapper.AppointmentMapper;
import com.yicheng.modules.appointment.pojo.entity.appointmentEntity;
import com.yicheng.modules.appointment.service.IDoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillConsumer {
    private final RabbitTemplate rabbitTemplate;
    private final IDoctorService doctorService;
    private final AppointmentMapper appointmentMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConfig.SECKILL_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleSeckillOrder(Map<String, Object> orderInfo) {

        Long doctorId = Long.valueOf(orderInfo.get("doctorId").toString());
        Long elderId = Long.valueOf(orderInfo.get("elderId").toString());
        String orderToken = orderInfo.get("orderToken").toString();

        //log.info("【MQ消费者】开始处理秒杀订单：专家ID={}, 老人ID={}", doctorId, elderId);

        //秒杀队列消费者 操作数据库 扣取库存
        boolean success = doctorService.update()
                .setSql("stock=stock-1")
                .eq("id", doctorId)
                .gt("stock", 0)
                .update();

        if (success) {
            // 1 如果扣减成功，生成预约记录 (下订单)
            appointmentEntity appointment = new appointmentEntity();
            appointment.setDoctorId(doctorId);
            appointment.setElderId(elderId);
            appointment.setCreateTime(LocalDateTime.now());
            appointment.setOrderSn(orderToken);
            appointment.setStatus(1); // 初始状态为1 待支付
            // 2插入数据库
            appointmentMapper.insert(appointment);
            Long orderId = appointment.getId();//插入后MP会自增ID 获取ID

            //3把订单Id放入map
            orderInfo.put("orderId", orderId);

            //发送延迟信息 给15分钟后的超时处理队列
            rabbitTemplate.convertAndSend(
                    RabbitConfig.ORDER_DELAY_EXCHANGE,
                    RabbitConfig.ORDER_DELAY_ROUTING,
                    orderInfo
            );

            //log.info("【MQ下单】订单创建成功，ID: {}，已进入15分钟支付倒计时", orderId);

        } else {
           //数据库扣减失败 就要回滚redis内存
            log.error("【警告】数据库库存已耗尽，请检查数据对账！");

            //归还库存
            String stockKey = RedisConstants.DOCTOR_STOCK_KEY + doctorId;
            stringRedisTemplate.opsForValue().increment(stockKey);

            //删除一人一单标志
            String successKey=RedisConstants.SECKILL_SUCCESS_KEY+elderId+":"+doctorId;
            stringRedisTemplate.delete(successKey);

            return;
        }
    }

}
