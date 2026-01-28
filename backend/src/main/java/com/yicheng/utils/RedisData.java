package com.yicheng.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    //用于逻辑过期
    private LocalDateTime expireTime;
    private Object Data;
}
