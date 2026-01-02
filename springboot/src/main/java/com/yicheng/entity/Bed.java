package com.yicheng.entity;


import lombok.Data;

@Data
public class Bed {
    Integer id;
    String bedNumber;// 床位号，如 "A-101"
    Integer status;// 状态：0-空闲，1-占用
}
