package com.yicheng.controller;

import cn.hutool.core.util.StrUtil;
import com.yicheng.common.Result;
import com.yicheng.entity.SysUser;
import com.yicheng.exception.CustomException;
import com.yicheng.service.UserService;
import com.yicheng.utils.JwtUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class WebController {

    @Resource
    private UserService userService;

    /**
     * 登录接口
     * */
    @PostMapping("/login")
    // 接收参数改成 SysUser (或者 Map)
    public Result<Map<String, Object>> login(@RequestBody SysUser user) {
        // 1. 调用 UserService 登录
        SysUser dbUser = userService.login(user);

        // 2. 生成 Token
        String token = JwtUtils.createToken(dbUser.getId().toString(), dbUser.getUsername());

        // 3. 返回
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        dbUser.setPassword(null); // 擦除密码
        map.put("user", dbUser);  // 返回的是 SysUser 对象

        return Result.success(map);
    }


    //注册接口
    @PostMapping("register")
    public Result<?> register(@RequestBody SysUser sysuser){
        userService.register(sysuser);
        return Result.success();
    }


    //更新密码接口
    @PutMapping("/updatePassword")
    public Result<?> updatePassword(@RequestBody Map<String, String> map) {
        // 1. 获取参数
        String oldPass = map.get("oldPass");
        String newPass = map.get("newPass");

        // 2. 参数校验
        if (StrUtil.isBlank(oldPass) || StrUtil.isBlank(newPass)) {
            return Result.error("400", "密码不能为空");
        }

        // 3. 获取当前登录用户 (利用你刚才写的静态工具方法)
        SysUser currentUser = JwtUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("401", "未登录");
        }

        // 4. 比对旧密码 (注意：这里是从数据库查出来的真实密码)
        if (!currentUser.getPassword().equals(oldPass)) {
            return Result.error("400", "旧密码错误，请检查");
        }

        // 5. 设置新密码并保存
        currentUser.setPassword(newPass);
        userService.update(currentUser); // 复用 Service 的 update 方法

        return Result.success();
    }

    /**
     * 修改个人信息 (昵称、头像)
     * 不需要校验旧密码，只要有 Token 就能改
     */
    @PutMapping("/updateUser")
    public Result<SysUser> updateUser(@RequestBody SysUser user) {
        // 1. 安全第一：从 Token 获取当前登录用户的 ID
        // 防止恶意用户在 JSON 里传别人的 ID (比如 id=1) 去修改管理员的头像
        SysUser currentUser = JwtUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("401", "未登录");
        }

        // 2. 强制将 ID 设为当前登录用户的 ID
        user.setId(currentUser.getId());

        // 3. 调用 Service 更新 (只更新非空字段)
        // 注意：这里复用的是 UserService 的 update 方法
        userService.update(user);

        // 4. 【重要】为了让前端能更新缓存里的头像，我们需要把最新的数据查出来返回去
        SysUser latestUser = userService.selectById(currentUser.getId());

        // 5. 安全擦除密码 (永远不要把密码返回给前端)
        latestUser.setPassword(null);

        return Result.success(latestUser);
    }
}