package com.yicheng.common;


import java.lang.annotation.*;

/**
 * 权限注解：只有管理员才能访问
 * 把这个注解加在 Controller 方法上，非管理员访问会直接报错
 */
@Target(ElementType.METHOD) // 作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时有效
@Documented
public @interface RequireRole {
    // 允许传入一个角色字符串，默认为 ADMIN
    String value() default RoleConstant.ADMIN;
}
