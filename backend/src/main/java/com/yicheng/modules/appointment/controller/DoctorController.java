package com.yicheng.modules.appointment.controller;


import com.yicheng.common.RedisConstants;
import com.yicheng.common.Result;
import com.yicheng.modules.appointment.pojo.entity.doctorEntity;
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
        String message = doctorService.seckill(doctorId, elderId);

        if (message.contains("成功") || message.contains("出票中")) {
            return Result.success(message);
        } else {
            return Result.error("500", message);
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

    @PostMapping("/update")
    @Operation(summary = "修改医生")
    public Result<String> update(@RequestParam Long doctorId) {
        String msg=doctorService.updateDoctorInfo(doctorId);

        if ("修改成功".equals(msg)) {
            return Result.success(msg);
        } else {
            // 如果是“正在修改”或者“系统异常”，统一返回 500
            return Result.error("500", msg);
        }
    }

}
