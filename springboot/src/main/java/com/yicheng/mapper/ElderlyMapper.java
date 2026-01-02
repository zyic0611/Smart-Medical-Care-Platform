package com.yicheng.mapper;

import com.yicheng.entity.Elderly;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ElderlyMapper {
    /**
     * 查询老人列表（同时带出护工信息和床位信息）
     * @param name 老人姓名 (用于搜索)
     */
    List<Elderly> selectAll(@Param("name") String name);

    Integer insert( Elderly elderly);

    Integer updateById(Elderly elderly);

    Integer deleteById(int id);


    Elderly selectById(Integer id);
}
