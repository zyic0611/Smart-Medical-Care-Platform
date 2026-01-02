package com.yicheng.common;

import cn.hutool.core.util.StrUtil;
import com.yicheng.exception.CustomException;
import com.yicheng.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器：负责检查每一个请求头里有没有带 Token
 */
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 1. 放行 OPTIONS 请求 (浏览器的预检请求，必须放行，否则跨域会挂)
        if("OPTIONS".equals(request.getMethod().toUpperCase())) {
            return true;
        }

        // 2. 从请求头 Header 中获取 token
        String token = request.getHeader("token");

        // 3. 如果没有 token，直接拦截
        if (StrUtil.isBlank(token)) {
            throw new CustomException("401", "无权限，请先登录");
        }

        // 4. 验证 token 真伪
        // 如果验证失败，validateToken 方法内部会抛出异常，GlobalExceptionHandler 会捕获
        JwtUtils.validateToken(token);

        // 5. 验证通过，放行
        return true;
    }
}