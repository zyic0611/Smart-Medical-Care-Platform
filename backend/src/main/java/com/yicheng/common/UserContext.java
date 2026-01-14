package com.yicheng.common;

import com.yicheng.modules.user.entity.SysUser;

public class UserContext {
    // 创建一个 ThreadLocal 变量
    private static final ThreadLocal<SysUser> USER_HOLDER = new ThreadLocal<>();

    // 存入用户
    public static void setUser(SysUser user) {
        USER_HOLDER.set(user);
    }

    // 获取用户
    public static SysUser getUser() {
        return USER_HOLDER.get();
    }

    // 必须有清除方法，防止内存泄漏（线程池中的线程会被复用）
    public static void clear() {
        USER_HOLDER.remove();
    }
}
