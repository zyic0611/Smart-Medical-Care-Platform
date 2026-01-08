package com.yicheng.common;


import com.yicheng.entity.SysUser;
import com.yicheng.exception.CustomException;
import com.yicheng.utils.JwtUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect // 1. 声明这是一个切面
@Component // 2. 交给 Spring 管理
public class PermissionAspect {

    /**
     * 前置通知 (@Before)：在目标方法执行【之前】先检查
     * @annotation(com.yicheng.common.RequireAdmin) 表示拦截所有加了这个注解的方法
     */
    @Before("@annotation(com.yicheng.common.RequireAdmin)")
    public void checkAdmin() {
        // 1. 获取当前登录用户 (利用你之前写的 ThreadLocal 神器)
        SysUser currentUser = JwtUtils.getCurrentUser();

        // 2. 双重保险：虽然拦截器拦截了未登录，但这里再判空一下更稳健
        if (currentUser == null) {
            throw new CustomException("401", "未登录或登录已过期");
        }

        // 3. 核心校验：判断角色是不是 ADMIN
        // 注意：数据库里存的是 "ADMIN" 还是 "admin"，要保持一致，推荐用 equalsIgnoreCase 忽略大小写
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            // 4. 如果不是管理员，直接抛异常，Controller 的方法根本不会执行
            throw new CustomException("403", "权限不足，请联系管理员");
        }

        // 5. 如果是管理员，方法正常结束，Spring 会自动放行去执行 Controller
    }
}
