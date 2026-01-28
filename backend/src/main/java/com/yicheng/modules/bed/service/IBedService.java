package com.yicheng.modules.bed.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.modules.bed.pojo.dto.BedAddDTO;
import com.yicheng.modules.bed.pojo.dto.BedUpdateDTO;
import com.yicheng.modules.bed.pojo.entity.BedEntity;
import com.yicheng.modules.bed.pojo.vo.BedVO;

import java.util.List;

public interface IBedService extends IService<BedEntity> {
    IPage<BedVO> selectBedPage(IPage<BedVO>page, BedVO bedVO);

    List<BedVO> selectFreeBeds(BedVO bedVO);

    boolean save(BedAddDTO bedUpdateDTO);

    boolean updateByDTO(BedUpdateDTO bedUpdateDTO);

    boolean deleteLogic(List<Long> ids);

    BedVO detail(Long id);
}
