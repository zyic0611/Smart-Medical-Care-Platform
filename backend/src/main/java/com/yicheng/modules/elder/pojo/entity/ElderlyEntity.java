package com.yicheng.modules.elder.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yicheng.modules.employee.entity.Employee;
import com.yicheng.modules.bed.pojo.entity.BedEntity;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("elderly")
public class ElderlyEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;
    private String gender;
    private Integer age;
    private String healthStatus;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate checkInDate;

    private Integer nurseId; // 外键
    private Integer bedId;   // 外键

    @TableLogic
    private Integer isDeleted;
}
