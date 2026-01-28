package com.yicheng.modules.bed.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yicheng.modules.bed.pojo.entity.BedEntity;
import com.yicheng.modules.bed.pojo.vo.BedVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BedMapper extends BaseMapper<BedEntity> {

    /**
     * 自定义分页查询
     */
    List<BedVO> selectBedPage(IPage<BedVO> page, @Param("bed") BedVO bed);

    /**
     * 查询空闲床位
     */
    List<BedVO> selectFreeBeds(@Param("bed") BedVO bed);
}


