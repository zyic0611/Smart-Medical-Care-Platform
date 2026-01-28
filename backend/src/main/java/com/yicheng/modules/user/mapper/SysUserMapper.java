package com.yicheng.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yicheng.modules.user.entity.SysUser;

public interface SysUserMapper extends BaseMapper<SysUser> {
    // 登录用的查重/查账号
    SysUser selectByUsername(String username);


}