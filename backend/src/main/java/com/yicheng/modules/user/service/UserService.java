package com.yicheng.modules.user.service;

import com.yicheng.modules.user.entity.SysUser;
import com.yicheng.exception.CustomException;
import com.yicheng.modules.user.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Resource
    private SysUserMapper sysUserMapper;

    // 登录逻辑
    public SysUser login(SysUser user) {
        // 1. 查用户
        SysUser dbUser = sysUserMapper.selectByUsername(user.getUsername());
        if (dbUser == null) {
            throw new CustomException("400", "账号或密码错误");
        }
        // 2. 比对密码
        if (!dbUser.getPassword().equals(user.getPassword())) {
            throw new CustomException("400", "账号或密码错误");
        }
        return dbUser;
    }

    // 注册逻辑 (管理员注册)
    public void register(SysUser user) {
        // ... 参考之前的 register 逻辑，只是换成了操作 sysUserMapper ...
        // 默认角色给 USER
        user.setRole("USER");
        sysUserMapper.insert(user);
    }

    // 修改用户信息
    public void update(SysUser user) {
        sysUserMapper.updateById(user);
    }

    public SysUser selectById(Integer id) {
        return sysUserMapper.selectById(id);
    }
}