package com.yicheng.modules.elderly.pojo.vo;

import com.yicheng.modules.elderly.pojo.entity.ElderlyEntity;
import lombok.Data;

@Data
public class ElderlyVO extends ElderlyEntity {

    //外键
    private String nurseName;

    private String nursePhone;

    private String bedNumber;

    //字典
    private String healthStatusStr;

    private String genderStr;
}
