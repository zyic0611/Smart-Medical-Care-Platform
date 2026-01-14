package com.yicheng.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil; // Hutool 的 IP 获取工具
import com.yicheng.modules.syslog.entity.SysLog;
import com.yicheng.modules.user.entity.SysUser;
import com.yicheng.modules.syslog.mapper.SysLogMapper;
import com.yicheng.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面
 * 作用：拦截所有加了 @AutoLog 注解的方法，自动记录日志
 */
@Aspect // 1. 声明这是一个切面
@Component // 2. 交给 Spring 管理
public class LogAspect {

    @Resource
    private SysLogMapper sysLogMapper;

    /**
     * 环绕通知 (@Around)：最强大的通知类型，可以在方法执行前后都做事
     * @param point 连接点 (代表原本要执行的那个方法)
     * @param autoLog 注解对象 (为了获取你在括号里写的 "新增老人" 这种描述)
     */
    @Around("@annotation(autoLog)")
    public Object doAround(ProceedingJoinPoint point, AutoLog autoLog) throws Throwable {

        // ---------------- 1. 方法执行前：掐表 ----------------
        long beginTime = System.currentTimeMillis();

        // 执行原来的方法 (比如 controller.add)
        // result 就是原本方法返回的结果 (Result 对象)
        Object result = point.proceed();

        // ---------------- 2. 方法执行后：算账 ----------------
        long time = System.currentTimeMillis() - beginTime; // 计算耗时(毫秒)

        // ---------------- 3. 异步/同步保存日志 ----------------
        saveLog(point, autoLog, time);

        return result;
    }

    /**
     * 保存日志的具体逻辑
     */
    private void saveLog(ProceedingJoinPoint point, AutoLog autoLog, long time) {
        SysLog log = new SysLog();

        // 1. 设置操作内容 (从注解里拿 value 值)
        if (autoLog != null) {
            log.setContent(autoLog.value());
        }

        // 2. 设置耗时
        log.setTime(time + "ms");

        // 3. 设置操作时间
        log.setCreateTime(LocalDateTime.now());

        // 4. 获取请求对象 (为了拿 IP)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // 使用 Hutool 获取真实 IP (能穿透 Nginx 代理)
            String ip = JakartaServletUtil.getClientIP(request);
            log.setIp(ip);
        }

        // 5. 设置操作人 (从 Token 获取)
        // 这里的 JwtUtils.getCurrentUser() 是你之前写好的静态方法
        SysUser currentUser = JwtUtils.getCurrentUser();
        if (currentUser != null) {
            // 优先存昵称，没有昵称存账号
            String username = StrUtil.isNotBlank(currentUser.getNickname()) ? currentUser.getNickname() : currentUser.getUsername();
            log.setUser(username);
        } else {
            log.setUser("未登录/未知"); // 比如注册接口就没有登录态
        }

        // 6. 插入数据库
        sysLogMapper.insert(log);
    }
}