package com.yicheng.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    //队列名称 每一个队列都是唯一的
    public static final String QUEUE_NAME = "medical_diagnosis_queue";

    @Bean
    public Queue aiQueue() {
        // true 表示持久化，RabbitMQ 重启后队列还在
        return new Queue(QUEUE_NAME, true);
    }
}