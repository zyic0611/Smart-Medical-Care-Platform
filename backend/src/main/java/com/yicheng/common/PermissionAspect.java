package com.yicheng.common;


import com.yicheng.modules.user.entity.SysUser;
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
     * @annotation(requireRole) 表示拦截这个类的实体
     */
    @Before("@annotation(requireRole)")//可以直接注入注解对象
    public void checkRole(RequireRole requireRole) {
        // 1. 获取当前登录用户 (利用ThreadLocal)
        SysUser currentUser = JwtUtils.getCurrentUser();

        // 2. 双重保险：虽然拦截器拦截了未登录，但这里再判空一下更稳健
        if (currentUser == null) {
            throw new CustomException("401", "未登录或登录已过期");
        }

        //3. 获取注解中需要的角色
        String expectedRole =requireRole.value();

        //4. 核心校验：判断角色是不是需要的
        //推荐用 equalsIgnoreCase 忽略大小写
        if (!expectedRole.equalsIgnoreCase(currentUser.getRole())) {
            // 5. 如果需要的角色，直接抛异常，Controller 的方法不会执行
            throw new CustomException("403", "权限不足，该操作需要"+expectedRole+"权限");
        }

    }
}
