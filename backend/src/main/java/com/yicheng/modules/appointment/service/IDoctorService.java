package com.yicheng.modules.appointment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.modules.appointment.pojo.entity.doctorEntity;

public interface IDoctorService extends IService<doctorEntity> {
    String seckill(Long doctorId, Long elderId);

    String updateDoctorInfo(Long doctorId);
}
