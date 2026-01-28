package com.yicheng.modules.user.controller;

import cn.hutool.core.util.StrUtil;
import com.yicheng.common.Result;
import com.yicheng.modules.user.dto.MobileLoginRequest;
import com.yicheng.modules.user.dto.SysUserDto;
import com.yicheng.modules.user.entity.SysUser;
import com.yicheng.modules.user.service.UserService;
import com.yicheng.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name="用户管理模块")
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 登录接口
     * */
    @PostMapping("/login")
    @Operation(summary ="用户登录接口")
    // 接收参数改成 SysUser (或者 Map)
    public Result<Map<String, Object>> login(@RequestBody SysUserDto user) {
        // 1. 调用 UserService 登录
        return Result.success(userService.login(user));
    }

    @PostMapping("/code")//发送短信接口
    @Operation(summary = "发送短信接口")
    public Result<String> code(@RequestParam String phone) {
        userService.sendcode(phone);
        return Result.success("发送验证码成功");
    }



    @PostMapping("/logout")
    @Operation(summary = "退出登陆接口")
    public Result<String> logout(HttpServletRequest request) {//必须传参数 因为不走拦截器 就拿不到threadlocal
        userService.logout(request);
        return Result.success("退出登录成功");
    }


    //注册接口
    @PostMapping("register")
    @Operation(summary ="用户注册接口")
    public Result<?> register(@RequestBody SysUser sysuser){
        userService.register(sysuser);
        return Result.success();
    }


    @PostMapping("/login/mobile")
    @Operation(summary = "手机登陆接口")
    public Result<Map<String,Object>> mobile(@RequestBody MobileLoginRequest mobileLoginRequest) {
        return Result.success(userService.loginByMobile(mobileLoginRequest.getPhone(),mobileLoginRequest.getCode()));
    }




    //更新密码接口
    @PutMapping("/updatePassword")
    @Operation(summary ="用户更新密码接口")
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
        userService.updateById(currentUser);

        return Result.success();
    }

    /**
     * 修改个人信息 (昵称、头像)
     * 不需要校验旧密码，只要有 Token 就能改
     */
    @PutMapping("/updateUser")
    @Operation(summary ="用户修改个人信息接口")
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
        userService.updateById(user);

        // 4. 【重要】为了让前端能更新缓存里的头像，我们需要把最新的数据查出来返回去
        SysUser latestUser = userService.getById(currentUser.getId());

        // 5. 安全擦除密码 (永远不要把密码返回给前端)
        latestUser.setPassword(null);

        return Result.success(latestUser);
    }
}