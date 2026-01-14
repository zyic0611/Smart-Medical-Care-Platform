package com.yicheng.modules.bed.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Bed {
    @TableId(value = "id", type = IdType.AUTO)
    Integer id;
    String bedNumber;// 床位号，如 "A-101"
    Integer status;// 状态：0-空闲，1-占用
}
