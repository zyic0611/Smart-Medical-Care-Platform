package com.yicheng.config;

import com.yicheng.common.JwtInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 1. 告诉 Spring Boot 这是一个配置类
public class WebConfig implements WebMvcConfigurer {

    //跨域配置
    /*
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 2. 对所有路径生效
                .allowedOrigins("http://localhost:5173") // 3. 允许前端地址
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 4. 允许的方法
                .allowedHeaders("*")
                .allowCredentials(true);
    }*/


    // 先把拦截器注入进容器  这样拦截器就可以调用spring中的各种功能
    @Bean
    public JwtInterceptor jwtInterceptor() {
        return new JwtInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor())
                .addPathPatterns("/**")        // 1. 拦截所有请求
                .excludePathPatterns("/login") // 2. 放行登录接口 (一定要放行，否则死循环)
                .excludePathPatterns("/register") // 3 放行注册
                .excludePathPatterns("/files/**")// 4 放行文件访问 (看头像不需要登录)
                .excludePathPatterns("/employee/export") // 5 放行导出为excell 由于window.open 没法自动带 Header 里的 token
                .excludePathPatterns("/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/favicon.ico");// 6 放行文档页面
    }
}