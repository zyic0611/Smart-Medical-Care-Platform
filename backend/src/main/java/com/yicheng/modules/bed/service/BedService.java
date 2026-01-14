package com.yicheng.modules.bed.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.modules.bed.entity.Bed;

import java.util.List;

public interface BedService extends IService<Bed> {
    IPage<Bed> selectPage(Integer pageNum, Integer pageSize, String bedNumber);

    List<Bed> selectFreeBeds();

    void add(Bed bed);

    void update(Bed bed);

    void delete(Integer id);
}
