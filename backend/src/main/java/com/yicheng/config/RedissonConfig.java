package com.yicheng.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        // 1. 创建配置对象
        Config config = new Config();

        // 2. 添加 Redis 地址（单机模式）
        // 如果你的 Redis 有密码，记得加上 .setPassword("你的密码")
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379");

        // 3. 创建 RedissonClient 实例并返回
        return Redisson.create(config);
    }
}
