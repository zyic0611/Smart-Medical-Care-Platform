package com.yicheng.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.entity.Elderly;
import org.springframework.transaction.annotation.Transactional;

public interface ElderlyService extends IService<Elderly> {
    IPage<Elderly> selectPage(Integer pageNum, Integer pageSize, String name);

    @Transactional(rollbackFor = Exception.class)
    void addElderlyWithBed(Elderly elderly);

    @Transactional(rollbackFor = Exception.class)
    void updateElderlyWithBed(Elderly elderly);

    @Transactional(rollbackFor = Exception.class)
    void deleteById(Integer id);
}
