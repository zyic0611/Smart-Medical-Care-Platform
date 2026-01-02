package com.yicheng.mapper;

import com.yicheng.entity.Bed;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BedMapper {

    //查找所有床位
    List<Bed> selectPage(@Param("bedNumber") String bedNumber);

    // 查询所有空闲床位 (status = 0)
    List<Bed> selectFreeBeds();

    // 根据ID更新状态
    void updateStatus(@Param("id") Integer id, @Param("status")Integer status);

    void insert(Bed bed);

    void deleteById(Integer id);

    Bed selectByNumber(String bedNumber);

    void updateById(Bed bed);

    Bed selectById(Integer id);
}


