package com.yicheng.modules.appointment.mq;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yicheng.common.RedisConstants;
import com.yicheng.config.RabbitConfig;
import com.yicheng.modules.appointment.mapper.AppointmentMapper;
import com.yicheng.modules.appointment.pojo.entity.appointmentEntity;
import com.yicheng.modules.appointment.service.IDoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TimeoutConsumer {

    private final AppointmentMapper appointmentMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final IDoctorService doctorService;

    //处理死信队列 也就是超时没支付的
    @RabbitListener(queues = RabbitConfig.ORDER_TIMEOUT_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderTimeout(Map<String, Object> orderInfo){

        //1 获取队列信息
        Long doctorId = Long.valueOf(orderInfo.get("doctorId").toString());
        Long orderId = Long.valueOf(orderInfo.get("orderId").toString());
        Long elderId = Long.valueOf(orderInfo.get("elderId").toString());

        //log.info("【死信监听】接收到超时消息，准备处理 OrderID: {}", orderId);


        //2执行原子更新 只有状态仍然为1 未支付 就要变成5 超时未支付

        UpdateWrapper<appointmentEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", 5)
                .eq("id", orderId)
                .eq("status", 1);


        int rows=appointmentMapper.update(null,updateWrapper);
        if(rows>0){
            //更新成功 代表真的超时 归还redis库存
            String stockKey= RedisConstants.DOCTOR_STOCK_KEY + doctorId;
            stringRedisTemplate.opsForValue().increment(stockKey);
            //归还数据库库存
            doctorService.update()
                    .setSql("stock=stock+1")
                    .eq("id", doctorId)
                    .gt("stock", 0)
                    .update();
            //还需要删除一人一单标志
            String successKey=RedisConstants.SECKILL_SUCCESS_KEY+elderId+":"+doctorId;
            stringRedisTemplate.delete(successKey);
        }


    }
}
