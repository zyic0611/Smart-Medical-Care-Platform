package com.yicheng.modules.medicalimg.mapper;

import com.yicheng.modules.medicalimg.entity.MedicalImaging;
import org.apache.ibatis.annotations.Param;

import java.util.List;



public interface ImageMapper {

     MedicalImaging selectById(@Param("id") Integer id);

     List<MedicalImaging> selectByElderId(@Param("elderId") Integer id);

    void insert(MedicalImaging medicalImaging);
}
