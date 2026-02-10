package com.yicheng.modules.appointment.controller;


import com.yicheng.common.RedisConstants;
import com.yicheng.common.Result;
import com.yicheng.modules.appointment.pojo.entity.doctorEntity;
import com.yicheng.modules.appointment.pojo.vo.doctorRankVO;
import com.yicheng.modules.appointment.service.IDoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctor")
@RequiredArgsConstructor
@Tag(name="专家号管理模块")
public class DoctorController {

    private final IDoctorService doctorService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 1. 获取所有专家列表 (用于前端展示库存)
     */
    @GetMapping("/list")
    @Operation(summary = "列表查询所有专家")
    public Result<List<doctorEntity>> list() {
        return Result.success(doctorService.list());
    }

    /**
     * 2. 核心：专家预约抢号接口 (乐观锁测试)
     * @param doctorId 专家ID
     * @param elderId  当前操作的老人/家属ID
     * //
     */
    @PostMapping("/seckill")
    @Operation(summary = "预约抢号")
    public Result<String> seckill(@RequestParam Long doctorId, @RequestParam Long elderId) {
        // 调用我们之前写好的包含乐观锁逻辑的 Service
        String result    = doctorService.seckill(doctorId, elderId);

        // 根据前缀判断是错误还是 Token
        if (result.startsWith("err:")) {
            return Result.error("500", result.replace("err:", ""));
        } else {
            // 返回的是 orderToken
            return Result.success(result);
        }
    }

    @PostMapping("/warmup")
    @Operation(summary = "缓存预热")
    public Result<String> warmup(@RequestParam Long doctorId) {

        //0 .先执行删除，确保清理掉之前的负数或脏数据
        String key= RedisConstants.DOCTOR_STOCK_KEY+doctorId;
        stringRedisTemplate.delete(key);


        //1 .查数据库
        doctorEntity doctor = doctorService.getById(doctorId);
        if (doctor == null) {
            return Result.error("500","专家号不存在");
        }

        //2 .存入redis 形式为id,stock

        stringRedisTemplate.opsForValue().set(key,String.valueOf(doctor.getStock()));

        return Result.success("预热成功，redis专家号缓存已加载"+doctor.getStock()+"个号");
    }

    @PostMapping("/result/{orderToken}")
    @Operation(summary = "轮询抢号结果")
    public Result<Object>getSeckillResult(@PathVariable String orderToken){
        return doctorService.getSeckillResult(orderToken);
    }



    @PostMapping("/update/doctorStock")
    @Operation(summary = "更新专家号库存")
    public Result<String> updateDoctorStock(@RequestParam Long doctorId, @RequestParam Integer stock) {
        boolean result=doctorService.updateDoctorStock(doctorId,stock);
        if (result)
            return Result.success("更新专家号库存成功,当前库存:"+stock);
        return Result.error("500","库存更新失败，专家号不存在");
    }

    @PostMapping("/pay/{orderToken}")
    @Operation(summary = "支付订单")
    public Result<String> pay(@PathVariable String orderToken){
        boolean result=doctorService.payAppointment(orderToken);
        if(result)
            return Result.success("支付订单成功，预约成功！");
        return Result.error("500","请重新支付");
    }

    @PostMapping("/hot-rank")
    @Operation(summary = "热门排行")
    public Result<List<doctorRankVO>> getHotRank(){
        return Result.success(doctorService.getHotRank());
    }

}
