package com.yicheng.modules.bed.pojo.vo;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class BedVO  implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    Integer id;
    String bedNumber;// 床位号，如 "A-101"
    Integer status;// 状态：0-空闲，1-占用

    private String statusName;
}
