package com.yicheng.modules.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.modules.user.dto.SysUserDto;
import com.yicheng.modules.user.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface UserService extends IService<SysUser> {
    // 登录逻辑
    Map<String, Object> login(SysUserDto user);

    // 注册逻辑 (管理员注册)
    void register(SysUser user);


    boolean sendcode(String phone);

    Map<String,Object> loginByMobile(String phone, String code);

    void logout(HttpServletRequest request);
}
