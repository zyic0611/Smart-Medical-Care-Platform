package com.yicheng.modules.hospital.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.modules.hospital.pojo.entity.HospitalEntity;
import com.yicheng.modules.hospital.pojo.vo.HospitalVO;

import java.util.List;


/**
 * (Hospital)表服务接口
 *
 * @author zyc
 * @since 2026-02-10 14:37:49
 */
public interface IHospitalService extends IService<HospitalEntity> {

    List<HospitalVO> getNearBy(Double longitude, Double latitude, Double range);
}

