package com.yicheng.modules.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.yicheng.modules.hospital.pojo.entity.HospitalEntity;
import com.yicheng.modules.hospital.pojo.vo.HospitalVO;

/**
 * (Hospital)表数据库访问层
 *
 * @author zyc
 * @since 2026-02-10 14:37:49
 */
public interface HospitalMapper extends BaseMapper<HospitalEntity> {

}

