package com.yicheng.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.yicheng.common.UserContext;
import com.yicheng.modules.user.entity.SysUser;
import com.yicheng.exception.CustomException;
import com.yicheng.modules.user.mapper.SysUserMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;

//1 必须加上 @Component，让 Spring 接管它
@Component
public class JwtUtils {

    // 密钥：千万不能告诉别人！就像你的私章。实际开发中应该存在配置文件里
    private static final String SECRET = "my-super-secret-key-123";

    // 过期时间：12小时 (单位毫秒)
    private static final long EXPIRE_TIME = 100L * 365 * 24 * 60 * 60 * 1000; //修改为100年 方便接口测试



    // 2 定义一个静态的 Mapper 变量   (实现静态注入的案例)
    private static SysUserMapper staticSysUserMapper;


    // 3注入普通的 Mapper (注意：这里不能加 static)
    @Resource
    private SysUserMapper sysuserMapper;

    // 4  使用 @PostConstruct生命周期钩子 保证是在依赖注入后执行构造  初始化静态变量
    // 原理：当 Spring 启动并创建好这个类后，会自动执行这个 init 方法，
    // 我们趁机把注入进来的 employeeMapper 赋值给静态的 staticEmployeeMapper
    @PostConstruct
    public void init() {
        staticSysUserMapper = sysuserMapper;
    }

    /**
     * 生成 Token
     * @param userId 用户ID
     * @param username 用户名
     * @return 加密后的 Token 字符串
     */

    //创建token
    public static String createToken(String userId, String username) {
        return JWT.create()
                .withAudience(userId) // 把用户ID存进 Token 里（任何人都能看到，但改不了）
                .withClaim("username", username) // 把用户名也存进去
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME)) // 设置过期时间
                .sign(Algorithm.HMAC256(SECRET)); // 用密钥签名，防止篡改
    }

    /**
     * 校验 Token 并获取当前登录的用户ID
     * @return 用户ID
     */
    public static String validateToken(String token) {
        try {
            // 1. 验证 Token 是否合法（也就是验证是不是我签发的，有没有过期）
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(SECRET)).build().verify(token);

            // 2. 如果验证通过，获取里面存的 UserID
            return jwt.getAudience().get(0);

        } catch (Exception e) {
            // 如果验证失败（过期、被篡改），抛出业务异常
            throw new CustomException("401", "Token 验证失败，请重新登录");
        }
    }

    /*
        5. 使用静态注入 实现的 getCurrentUser 方法
         获取当前登录用户信息
    */
    public static SysUser getCurrentUser() {
        // 直接从 ThreadLocal 中获取，这只是内存操作，速度极快！
        return UserContext.getUser();
    }

    // 顺便写一个方便拦截器调用的方法
    public static SysUser getUserById(Integer userId) {
        return staticSysUserMapper.selectById(userId);
    }
}