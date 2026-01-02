package com.yicheng.common;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // 作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时有效
@Documented
public @interface AutoLog {
    String value() default ""; // 用来存操作描述，比如 "新增老人"
}