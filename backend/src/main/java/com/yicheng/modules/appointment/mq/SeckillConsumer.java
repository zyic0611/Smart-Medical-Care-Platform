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

        //log.info("【MQ消费者】开始处理秒杀订单：专家ID={}, 老人ID={}", doctorId, elderId);

        boolean success = doctorService.update()
                .setSql("stock=stock-1")
                .eq("id", doctorId)
                .gt("stock", 0)
                .update();

        if (success) {
            // 如果扣减成功，生成预约记录 (下订单)
            appointmentEntity appointment = new appointmentEntity();
            appointment.setDoctorId(doctorId);
            appointment.setElderId(elderId);
            appointment.setCreateTime(LocalDateTime.now());
            appointment.setStatus(1); // 预约成功

            appointmentMapper.insert(appointment);
            //log.info("【MQ消费者】下单成功！老人ID: {}", elderId);
        } else {
            // 这种情况极少发生（除非 Redis 和 DB 数据严重不一致，比如 DB 被人手动改了）
            // 只有这时候才需要回滚 Redis
            //log.error("【MQ消费者】DB扣减失败！可能出现数据同步问题。准备回滚 Redis...");
            String stockKey = RedisConstants.DOCTOR_STOCK_KEY + doctorId;
            stringRedisTemplate.opsForValue().increment(stockKey);

            // 这里的报错会触发事务回滚，如果配置了重试机制，消息可能会重新入队
            throw new RuntimeException("秒杀库存同步异常");
        }
    }

}
