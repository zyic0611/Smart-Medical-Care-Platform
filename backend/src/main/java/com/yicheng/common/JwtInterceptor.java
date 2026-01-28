package com.yicheng.common;

import cn.hutool.core.util.StrUtil;
import com.yicheng.modules.user.entity.SysUser;
import com.yicheng.exception.CustomException;
import com.yicheng.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器：负责检查每一个请求头里有没有带 Token
 */
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 放行 OPTIONS
        if("OPTIONS".equals(request.getMethod().toUpperCase())) return true;

        // 2. 获取并验证 token
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) throw new CustomException("401", "请登录");

        // 3. JWT验证并解析出 userId
        String userId = JwtUtils.validateToken(token);

        //4.校验redis中该JWT是否过期
        String redisToken=stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_TOKEN_KEY+userId);

        //如果查不到则过期了token
        if (StrUtil.isBlank(redisToken)) {
            throw new CustomException("401", "登录已失效，请重新登录");
        }
        //如果token不同则该id在别的地方发了token
        if (!redisToken.equals(token)) {
            throw new CustomException("401", "您的账号在别处登录，请重新登录");
        }

        // 5. 把用户信息存入threadlocal
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