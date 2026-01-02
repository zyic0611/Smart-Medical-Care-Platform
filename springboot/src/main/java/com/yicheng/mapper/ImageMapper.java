package com.yicheng.mapper;

import com.yicheng.entity.MedicalImaging;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;



public interface ImageMapper {

     MedicalImaging selectById(@Param("id") Integer id);

     List<MedicalImaging> selectByElderId(@Param("elderId") Integer id);

    void insert(MedicalImaging medicalImaging);
}
