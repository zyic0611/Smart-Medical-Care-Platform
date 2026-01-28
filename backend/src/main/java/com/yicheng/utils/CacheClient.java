package com.yicheng.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
   //构造线程池 用于逻辑过期开启子线程读取数据库

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     方法1:将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
     */
    public void set(String key, Object value,Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 方法2:将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间
     * 用于处理缓存击穿问题
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //1 封装RedisData
        RedisData redisData = new RedisData();
        redisData.setData(value);
        //设置逻辑过期时间 当前时间+过期时间
        redisData.setData(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        //2 写入redis 不设置ttl
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 方法3：根据指定的key查询缓存，利用缓存空值的方式解决缓存穿透问题
     * @param keyPrefix key前缀
     * @param id 查询ID
     * @param type 返回值的类型 Class
     * @param dbFallback 数据库查询逻辑 (函数式编程)
     * @param time 过期时间
     * @param unit 时间单位
     * @param <R> 返回值类型
     * @param <ID> ID类型
     */
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id,Class<R>type, Function<ID,R>dbFallback,
                                       Long time, TimeUnit unit) {
        //拼接key
        String key = keyPrefix + id;

        //1 用key从redis查询
        String value=stringRedisTemplate.opsForValue().get(key);

        //2 判断是否存在
        if(StrUtil.isNotEmpty(value)){
            //3 存在 直接返回
            return JSONUtil.toBean(value, type);
        }

        //4 判断是否为空值 处理缓存穿透
        if(value==null){
            return null;
        }

        //5.不存在 就查询数据库
        R r=dbFallback.apply(id);

        //6 如果数据库也查不到 返回错误
        if(r==null){
            //将空值写入redis 防止缓存穿透 设置一个较短的ttl 2min
            stringRedisTemplate.opsForValue().set(key,"",2L,TimeUnit.MINUTES);
            //返回空
            return null;
        }

        //如果存在 则将数据库信息写入redis
        this.set(key,r,time,unit);//调用封装好的存入函数

        return r;


    }

    /**
     * 方法4：根据指定的key查询缓存，需要利用逻辑过期解决缓存击穿问题
     * 注意：使用此方法前，需要先预热数据（手动调用方法2存入数据），否则查不到直接返回null
     */
    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id,Class<R>type, Function<ID,R>dbFallback,
                                           Long time, TimeUnit unit){
        //1 从redis查询商户信息
        String key = keyPrefix + id;
        String value=stringRedisTemplate.opsForValue().get(key);

        //2判断是否存在
        if(!StrUtil.isNotEmpty(value)){
            //3不存在 直接null (逻辑过期要求必须存在redis中)
            return JSONUtil.toBean(value, type);
        }

        //4 命中 需要把查询到的对象反序列化为RedisData对象
        RedisData redisData = JSONUtil.toBean(value, RedisData.class);
        R r=JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        //5 判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //5.1 未过期 直接返回商户信息
            return r;
        }

        //6 过期了 需要缓存重建
        //6.1 获取互斥锁
        String lockKey="lock:"+key;
        boolean isLock=tryLock(lockKey);

        //6.2判断是否获得锁成功
        if(isLock){
            //6.3 如果成功 开启子线程 实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //查询数据库
                    R newR=dbFallback.apply(id);
                    //重建缓存
                    this.setWithLogicalExpire(key,newR,time,unit);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unLock(lockKey);
                }

            });
        }

        //6.4 返回过期的店铺信息 。（无论获取锁是否成功 都先返回旧数据）
        return r;



    }

    public void delete(String key){
        stringRedisTemplate.delete(key);
    }
    
    private boolean tryLock(String key){
        Boolean flag=stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
        return BooleanUtils.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }


}
