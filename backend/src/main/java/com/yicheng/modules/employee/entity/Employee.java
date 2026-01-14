package com.yicheng.modules.employee.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Employee {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;         // 数据库是 int
    private String name;
    private String gender;
    private Integer age;
    private String phone;
    private String address;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createTime;

    private String avatar; // 头像地址

}
