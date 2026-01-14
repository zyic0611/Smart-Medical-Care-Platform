package com.yicheng.common;

import cn.hutool.core.util.StrUtil;
import com.yicheng.modules.user.entity.SysUser;
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
        // 1. 放行 OPTIONS
        if("OPTIONS".equals(request.getMethod().toUpperCase())) return true;

        // 2. 获取并验证 token
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) throw new CustomException("401", "请登录");

        // 3. 验证并解析出 userId
        String userId = JwtUtils.validateToken(token);

        // 4. 【关键优化】在这里查一次库，并存入 UserContext
        // 注意：这里建议直接调用你的 UserService 或 Mapper
        SysUser currentUser = JwtUtils.getUserById(Integer.valueOf(userId));
        UserContext.setUser(currentUser);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 5. 【非常重要】请求结束后，清理 ThreadLocal
        UserContext.clear();
    }
}