package com.yicheng.mapper;

import com.yicheng.entity.SysUser;
import org.apache.ibatis.annotations.Param;

public interface SysUserMapper {
    // 登录用的查重/查账号
    SysUser selectByUsername(String username);

    // 注册/新增用户
    int insert(SysUser sysUser);

    // 根据ID查用户 (给 Token 解析用)
    SysUser selectById(Integer id);

    // 修改密码/头像用
    int updateById(SysUser sysUser);
}