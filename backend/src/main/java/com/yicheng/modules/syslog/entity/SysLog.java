package com.yicheng.modules.syslog.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysLog {
    Integer id;
    String content;
    String user;
    String ip;
    String time;
    LocalDateTime createTime;
}
