package com.yicheng.modules.diagnosis.service;

import com.yicheng.config.RabbitConfig;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DiagnosisService implements DiagnosisServiceIml{

    @Resource
    private RabbitTemplate rabbittemplate;

    @Override
    public void sendToAI(String taskId, String imagePath) {
        // 构造消息对象（对应 Python 里的 json.loads）
        Map<String, Object> message = new HashMap<>();
        message.put("task_id", taskId);
        message.put("image_path", imagePath);
        message.put("doctor_name", "王医生");

        // 发送消息到队列
        rabbittemplate.convertAndSend(RabbitConfig.QUEUE_NAME, message);
        System.out.println(" [📩] 诊断任务已推送到 RabbitMQ: " + taskId);
    }

}
