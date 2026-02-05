package com.yicheng.modules.appointment.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("doctor")
public class doctorEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String avatar;
    private String title;
    private String description;

    // 库存 (剩余号源)
    private Integer stock;

    private BigDecimal price;

    // 🔥 核心：乐观锁注解
    @Version
    private Integer version;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
