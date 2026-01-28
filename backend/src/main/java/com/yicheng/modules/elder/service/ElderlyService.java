package com.yicheng.modules.elder.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.modules.elder.pojo.entity.ElderlyEntity;
import org.springframework.transaction.annotation.Transactional;

public interface ElderlyService extends IService<ElderlyEntity> {
    IPage<ElderlyEntity> selectPage(Integer pageNum, Integer pageSize, String name);

    @Transactional(rollbackFor = Exception.class)
    void addElderlyWithBed(ElderlyEntity elderlyEntity);

    @Transactional(rollbackFor = Exception.class)
    void updateElderlyWithBed(ElderlyEntity elderlyEntity);

    @Transactional(rollbackFor = Exception.class)
    void deleteById(Integer id);

    IPage<ElderlyEntity> listElderlyWithImagingByPage(Integer pageNum, Integer pageSize);
}
