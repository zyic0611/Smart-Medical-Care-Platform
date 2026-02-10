package com.yicheng.modules.appointment.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.common.RedisConstants;
import com.yicheng.common.Result;
import com.yicheng.config.RabbitConfig;
import com.yicheng.modules.appointment.mapper.AppointmentMapper;
import com.yicheng.modules.appointment.mapper.DoctorMapper;
import com.yicheng.modules.appointment.pojo.entity.appointmentEntity;
import com.yicheng.modules.appointment.pojo.entity.doctorEntity;
import com.yicheng.modules.appointment.pojo.vo.appointmentVO;
import com.yicheng.modules.appointment.pojo.vo.doctorRankVO;
import com.yicheng.modules.sysdict.service.ISysDictService;
import com.yicheng.utils.Func;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    //字典
    private final ISysDictService sysDictService;

    //lua
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        // 设置脚本位置
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        // 设置返回值类型
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;


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
        if (res == -1) return "err:抢号太频繁了";
        if (res == -2) return "err:您已经抢过该号源";
        if (res == -3) return "err:号源已空";

        //4.生成唯一凭证orderToken
        // 使用 Java 自带的 UUID，去掉横杠
        String orderToken = java.util.UUID.randomUUID().toString().replace("-", "");

        // 5. 到这里说明抢到名额了，直接发 MQ 下单

        //封装信息
        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("doctorId", doctorId);
        orderInfo.put("elderId", elderId);
        orderInfo.put("orderToken", orderToken);

        rabbitTemplate.convertAndSend(RabbitConfig.SECKILL_QUEUE, orderInfo);

        return orderToken;//返回token给前端

    }

    @Override
    public Result<Object> getSeckillResult(String orderToken){

        //根据token去数据库查询订单
        appointmentEntity appointment=appointmentMapper.selectOne(
                new LambdaUpdateWrapper<appointmentEntity>()
                        .eq(appointmentEntity::getOrderSn, orderToken)
        );

        //逻辑判断

        //0:出票
        if(appointment==null){
            //没查到订单 说明可能还在队列里
            return Result.success("0","正在出票中，请稍等");
        }

        //转换为VO并包装 供返回前端
        appointmentVO vo = new appointmentVO();
        BeanUtil.copyProperties(appointment, vo);

        // 包装成 List 以适配你写的 setParamStr 方法
        List<appointmentVO> voList = Collections.singletonList(vo);
        this.setParamStr(voList);

        //1:抢到 待支付
        if(appointment.getStatus()==1){
            return Result.success("1",voList);
        }

        if (appointment.getStatus() == 2) {
            return Result.success("2",voList);
        }



        //500: 超时
        if (appointment.getStatus() == 5) {
            return Result.error("500", "订单支付超时，已被自动取消");
        }


        //500:异常
        // 情况 D: 其他异常状态
        return Result.error("500", "抢号失败，请重新尝试");
    }

    @Override
    public boolean updateDoctorStock(Long doctorId, Integer newStock) {
        //先更新数据库
        doctorEntity doctorEntity = this.getById(doctorId);
        if(doctorEntity==null){
            return false;
        }
        doctorEntity.setStock(newStock);
        boolean result=this.updateById(doctorEntity);

        //再更新redis
        if(result){
            String key=RedisConstants.DOCTOR_STOCK_KEY + doctorId;
            stringRedisTemplate.opsForValue().set(key, String.valueOf(newStock));
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payAppointment(String orderSn){
        //先查出订单
        appointmentEntity appointment=appointmentMapper.selectOne(new LambdaQueryWrapper<appointmentEntity>()
                .eq(appointmentEntity::getOrderSn, orderSn));
        //防御性编程
        if(appointment==null){
            return false;//订单不存在 直接返回
        }
        //原子更新 1->2
        UpdateWrapper<appointmentEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_sn", orderSn)
                .eq("status", 1)
                .set("status", 2)
                .set("pay_time", LocalDateTime.now());

        int rows=appointmentMapper.update(null,updateWrapper);
        if(rows>0){

            stringRedisTemplate.opsForZSet().incrementScore(
                    RedisConstants.DOCTOR_HOT_KEY,
                    appointment.getDoctorId().toString(),
                    1
            );
            return true;
        }
        return false;
    }

    @Override
    public List<doctorRankVO> getHotRank(){
        //用zset获取redis里排名前10的医生
        Set<ZSetOperations.TypedTuple<String>> typedTuples=stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisConstants.DOCTOR_HOT_KEY,0,9);
        //返回的是value:id ,score
        if(CollectionUtils.isEmpty(typedTuples)){
            return Collections.emptyList();
        }

        //解析数据并批量查询医生姓名
        List<Long>ids=typedTuples.stream()
                .map(tuple->Long.valueOf(tuple.getValue()))
                .collect(Collectors.toList());
        Map<Long,String>nameMap=this.listByIds(ids).stream()
                .collect(Collectors.toMap(doctorEntity::getId,doctorEntity::getName));
        return typedTuples.stream()
                .map(
                        tuple->{
                            doctorRankVO vo=new doctorRankVO();
                            Long id=Long.valueOf(tuple.getValue());
                            vo.setDoctorId(id);
                            vo.setDoctorName(nameMap.getOrDefault(id,"未知医生"));
                            vo.setBookingCount(tuple.getScore());

                            return vo;
                        }
                ).toList();

    }

    public void setParamStr(List<appointmentVO>list){
        if(Func.isEmpty(list)){
            return;
        }
        List<Long>doctorIds=list.stream()
                .map(appointmentVO ::getDoctorId)
                .filter(Func::isNotEmpty)
                .distinct()
                .toList();

        final Map<Long, String> doctorNameMap = Func.isNotEmpty(doctorIds)
                ? this.listByIds(doctorIds).stream()
                .collect(Collectors.toMap(doctorEntity::getId, doctorEntity::getName))
                : new HashMap<>();

        list.forEach(vo -> {
            vo.setDoctorName(doctorNameMap.getOrDefault(vo.getDoctorId(),"未知医生"));
            vo.setStatusStr(sysDictService.getValue("appointment_status",Func.toStr(vo.getStatus())));
        });

    }
}
