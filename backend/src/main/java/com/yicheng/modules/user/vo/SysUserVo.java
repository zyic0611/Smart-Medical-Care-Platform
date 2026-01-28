package com.yicheng.modules.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUserVo {
    private Integer id;
    private Integer linkId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private LocalDateTime createTime;
    private String phone;
}
