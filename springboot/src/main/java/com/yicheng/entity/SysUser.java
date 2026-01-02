package com.yicheng.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String role;
    private LocalDateTime createTime;
}