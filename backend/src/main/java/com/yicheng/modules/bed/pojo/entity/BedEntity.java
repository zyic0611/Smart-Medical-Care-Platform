package com.yicheng.modules.bed.pojo.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

@Data
public class BedEntity {
    @TableId(value = "id", type = IdType.AUTO)
    Integer id;
    String bedNumber;// 床位号，如 "A-101"
    Integer status;// 状态：0-空闲，1-占用

    @TableLogic
    private Integer isDeleted;
}

