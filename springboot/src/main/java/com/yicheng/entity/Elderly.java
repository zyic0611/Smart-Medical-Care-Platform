package com.yicheng.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Elderly {
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

    private Employee nurse; // 这里存护工的完整信息 (名字、电话等)
    private Bed bed;        // 这里存床位的完整信息 (床号、状态)
}
