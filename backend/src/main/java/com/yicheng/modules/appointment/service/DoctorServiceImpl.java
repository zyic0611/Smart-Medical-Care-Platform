package com.yicheng.modules.appointment.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.common.RedisConstants;
import com.yicheng.config.RabbitConfig;
import com.yicheng.modules.appointment.mapper.DoctorMapper;
import com.yicheng.modules.appointment.pojo.entity.doctorEntity;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
public class DoctorServiceImpl
extends ServiceImpl<DoctorMapper, doctorEntity>
implements IDoctorService {

    //redis
    private final StringRedisTemplate stringRedisTemplate;
    //rabbitmq
    private final RabbitTemplate rabbitTemplate;
    //redisson
    private final RedissonClient redissonClient;

    //lua
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        // 设置脚本位置
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        // 设置返回值类型
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    @Override
    public String seckill(Long doctorId, Long elderId){

        // 1. 准备 KEYS (对应脚本里的 KEYS[1, 2, 3])
        List<String> keys = Arrays.asList(
                RedisConstants.DOCTOR_STOCK_KEY + doctorId,
                RedisConstants.SECKILL_SUCCESS_KEY + elderId + ":" + doctorId,
                RedisConstants.SECKILL_LIMIT_KEY + elderId
        );

        // 2. 执行脚本 (参数 2 代表限流 2 秒)
        // 返回值对应 Lua 脚本里的 return -1, -2, -3, 1
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, keys, "2");

        // 3. 根据结果判断
        int res = result.intValue();
        if (res == -1) return "抢号太频繁了 请稍后再试";
        if (res == -2) return "您已经抢过该号源 不可重复购买";
        if (res == -3) return "抢号失败 号源已空(Lua拦截)";

        // 4. 到这里说明抢到名额了，直接发 MQ 下单
        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("doctorId", doctorId);
        orderInfo.put("elderId", elderId);
        rabbitTemplate.convertAndSend(RabbitConfig.SECKILL_QUEUE, orderInfo);

        return "正在出票中... 请稍等";

    }

    @Override
    public String updateDoctorInfo(Long doctorId){

        //1定义锁的key
        String lockKey=RedisConstants.LOCK_DOCTOR_UPDATE_KEY + doctorId;

        //2获取锁对象
        RLock lock=redissonClient.getLock(lockKey);

        //3尝试加锁
        // 参数含义：等待锁的时间(0s)，锁持有的自动释放时间(10s)，时间单位
        // 注意：如果不设释放时间，Redisson 会启动看门狗自动续期！建议测试时设为 -1 观察看门狗。
        boolean isLock=false;
        try {
            isLock=lock.tryLock(0,-1,TimeUnit.SECONDS);
            if(isLock){
                //加锁成功
                System.out.println("管理员获取锁成功，正在修改医生信息...");
                // 模拟数据库操作
                Thread.sleep(20000);
                return "修改成功";
            }else{
                // 获取锁失败
                return "当前有其他管理员正在修改，请稍后再试";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "系统异常";
        }finally {
            // 4.释放锁（必须放在 finally，且判断锁是否还在且由当前线程持有）
            if (isLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
                System.out.println("锁已释放");
            }
        }
    }
}
