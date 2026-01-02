package com.yicheng.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智慧医疗与养老护理平台接口文档") // 这里写你项目的名字
                        .version("1.0")
                        .description("包含老人管理、医学影像上传、YOLO识别等核心接口"));
    }
}
