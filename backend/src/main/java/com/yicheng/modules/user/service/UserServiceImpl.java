package com.yicheng.modules.user.service;

import ch.qos.logback.core.util.TimeUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.common.RedisConstants;
import com.yicheng.common.UserContext;
import com.yicheng.exception.CustomException;
import com.yicheng.modules.user.dto.SysUserDto;
import com.yicheng.modules.user.entity.SysUser;
import com.yicheng.modules.user.mapper.SysUserMapper;
import com.yicheng.modules.user.vo.SysUserVo;
import com.yicheng.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper,SysUser> implements UserService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    // 登录逻辑
    @Override
    public Map<String, Object> login(SysUserDto user) {
        // 1. 查用户
        SysUser dbUser = this.lambdaQuery().eq(SysUser::getUsername, user.getUsername()).one();

        //2 校验用户名是否存在
        if (dbUser == null) {
            throw new CustomException("400", "账号不存在");
        }
        // 3比对密码
        if (!dbUser.getPassword().equals(user.getPassword())) {
            throw new CustomException("400", "账号或密码错误");
        }

        //4登录成功 生成token
        String token =JwtUtils.createToken(dbUser.getId().toString(),dbUser.getUsername());

        //5存入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_TOKEN_KEY+dbUser.getId().toString(),token,12,TimeUnit.HOURS);


        //6封装返回结果
        Map<String,Object> map = new HashMap<>();
        map.put("token",token);

        // 【关键修改点】
        // 保护隐私：不要把数据库里的密码（哪怕是加密后的）返回给浏览器
        dbUser.setPassword(null);
        map.put("user",dbUser);
        return map;
    }

    // 注册逻辑 (管理员注册)
    @Override
    public void register(SysUser user) {
        // ... 参考之前的 register 逻辑，只是换成了操作 sysUserMapper ...
        // 默认角色给 USER
        user.setRole("USER");
        sysUserMapper.insert(user);
    }



    @Override
    public boolean sendcode(String phone) {
        //1校验手机格式
        if(StrUtil.isBlank(phone)|| !Validator.isMobile(phone)){
            throw new CustomException("400","无效手机号");
        }

        //2防刷（限流 防止重复发送短信 一分钟内）
        String limitKey= RedisConstants.LOGIN_LIMIT_KEY+phone;//唯一key
        Boolean hasLimit= stringRedisTemplate.hasKey(limitKey);
        if(hasLimit){
            throw new CustomException("400","发送太频繁，请一分钟后重试");
        }

        //3生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4存入redis  存验证码+防刷

        //4.1存入验证码 有效期5min
        String codeKey=RedisConstants.LOGIN_CODE_KEY+phone;
        stringRedisTemplate.opsForValue().set(
                codeKey,code,5, TimeUnit.MINUTES);

        //4.2防刷 有效期60s 代表这个手机号发过验证码了
        stringRedisTemplate.opsForValue().set(
                limitKey,code,1, TimeUnit.MINUTES
        );

        //5.模拟发送短信 实际得调用阿里云API
        log.info("智慧医疗】您的验证码是：{}，有效时间5分钟。",code);

        return true;
    }

    @Override
    public Map<String,Object> loginByMobile(String phone, String code) {

        //1手机号校验
        if(StrUtil.isBlank(phone)|| !Validator.isMobile(phone)){
            throw new CustomException("400","无效手机号");
        }

        //2取出redis里正确的code
        String targetCode=stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY+phone);

        //3校验验证码 判断如果为空或者不相等则不成功
        if(StrUtil.isBlank(targetCode)|| !targetCode.equals(code)){
            throw new CustomException("400","验证码过期或错误");
        }

        //4取出手机号对应的用户
        SysUser user = this.lambdaQuery().eq(SysUser::getPhone,phone).one();

        //如果用户为空 执行注册逻辑
        if(user==null){//
            user=new SysUser();
            user.setPhone(phone);
            user.setRole("USER");
            user.setPassword("123");
            user.setUsername("User_"+phone);//默认用户名为电话号码
            user.setNickname("用户_"+RandomUtil.randomNumbers(6));

            boolean save = this.save(user);
            if(!save){
                throw new RuntimeException("自动注册失败");
            }
        }

        //5 现在肯定有用户了 基于用户发jwt 生成token
        String token = JwtUtils.createToken(user.getId().toString(), user.getUsername());

        //把token存入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_TOKEN_KEY+user.getId().toString(),token,12, TimeUnit.HOURS);


        //生成map返回
        Map<String,Object> map=new HashMap<>();
        map.put("token",token);
        user.setPassword(null);
        map.put("user",user);

        return map;




    }

    @Override
    public void logout(HttpServletRequest request) {

        // 1. 手动从 header 拿 token
        String token = request.getHeader("token");

        if (StrUtil.isNotBlank(token)) {
            try {
                // 2. 解析 token 拿到 userId
                String userId = JwtUtils.validateToken(token);

                // 3. 删除 Redis
                stringRedisTemplate.delete(RedisConstants.LOGIN_TOKEN_KEY + userId);
                log.info("用户 {} 退出登录成功", userId);
            } catch (Exception e) {
                // 如果解析失败，说明 token 本身就是坏的，那也不用管了，直接当退出成功处理
                log.warn("退出登录时 Token 解析失败，可能已过期");
            }
        }

    }




}