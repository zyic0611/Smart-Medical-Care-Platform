package com.yicheng.modules.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yicheng.modules.appointment.pojo.entity.doctorEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DoctorMapper extends BaseMapper<doctorEntity> {
}