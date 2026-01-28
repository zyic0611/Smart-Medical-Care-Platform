package com.yicheng.modules.elder.pojo.vo;

import com.yicheng.modules.elder.pojo.entity.ElderlyEntity;
import lombok.Data;

@Data
public class ElderlyVO extends ElderlyEntity {


    private String nurseName;

    private String nursePhone;

    private String bedNumber;
}
