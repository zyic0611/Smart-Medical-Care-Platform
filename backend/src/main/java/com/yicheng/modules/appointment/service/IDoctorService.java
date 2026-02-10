package com.yicheng.modules.appointment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.common.Result;
import com.yicheng.modules.appointment.pojo.entity.doctorEntity;
import com.yicheng.modules.appointment.pojo.vo.doctorRankVO;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface IDoctorService extends IService<doctorEntity> {
    String seckill(Long doctorId, Long elderId);

    Result<Object> getSeckillResult(String orderToken);

    boolean updateDoctorStock(Long doctorId,Integer stock);

    boolean payAppointment(String orderSn);

    List<doctorRankVO> getHotRank();
}
