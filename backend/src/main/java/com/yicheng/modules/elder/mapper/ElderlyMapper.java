package com.yicheng.modules.elder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yicheng.modules.elder.pojo.entity.ElderlyEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;


public interface ElderlyMapper  extends BaseMapper<ElderlyEntity> {
    /**
     * 查询老人列表（同时带出护工信息和床位信息）
     * @param name 老人姓名 (用于搜索)
     */
    @ResultMap("ElderlyResultMap")
    IPage<ElderlyEntity> selectAll(IPage<ElderlyEntity> page, @Param("name") String name);


    @Select("""
        SELECT DISTINCT e.* FROM elderlyEntity e 
        INNER JOIN medical_img mi ON e.id = mi.elder_id 
        ORDER BY e.create_time DESC
        """)
    IPage<ElderlyEntity> listElderlyWithImagingByPage(IPage<ElderlyEntity> page);

}
