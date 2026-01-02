package com.yicheng.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Employee {
    private Integer id;         // 数据库是 int
    private String name;
    private String gender;
    private Integer age;
    private String phone;
    private String address;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createTime;

    private String avatar; // 头像地址

    private String role; // 对应数据库的 role 字段
}
