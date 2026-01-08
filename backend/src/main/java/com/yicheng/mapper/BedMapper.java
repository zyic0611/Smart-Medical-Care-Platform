package com.yicheng.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yicheng.entity.Bed;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BedMapper extends BaseMapper<Bed> {

    //查找所有床位
    IPage<Bed> selectPage(IPage<Bed> page, @Param("bedNumber") String bedNumber);

    // 查询所有空闲床位 (status = 0)
    List<Bed> selectFreeBeds();

    // 根据ID更新状态
    void updateStatus(@Param("id") Integer id, @Param("status")Integer status);


    void deleteById(Integer id);

    Bed selectByNumber(String bedNumber);


    Bed selectById(Integer id);
}


