package com.yicheng.modules.elder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yicheng.modules.employee.entity.Employee;
import com.yicheng.modules.bed.entity.Bed;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("elderly")
public class Elderly {

    @TableId(value = "id", type = IdType.AUTO)
    Integer id;
    String name;
    String gender;
    Integer age;
    String healthStatus;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate createTime; // 入住时间

    // ============ 2. 外键字段 (用来“存”数据的) ============
    // 当你新增/修改老人时，前端传给你的是 ID (比如护工ID=5, 床位ID=10)
    private Integer nurseId;
    private Integer bedId;

    // 告诉 MP：这个字段数据库里没有！是假的！
    @TableField(exist = false)
    private Employee nurse;
    @TableField(exist = false)
    private Bed bed;        // 这里存床位的完整信息 (床号、状态)
}
