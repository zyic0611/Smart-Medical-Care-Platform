package com.yicheng.modules.diagnosis.controller;


import com.yicheng.common.RequireRole;
import com.yicheng.common.Result;
import com.yicheng.common.RoleConstant;
import com.yicheng.modules.diagnosis.service.DiagnosisService;
import com.yicheng.utils.MinioUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("api/diagnosis")
@Tag(name="AI诊断接口")
public class DiagnosisController {

    @Resource
    private DiagnosisService diagnosisService;


    @Resource
    private MinioUtils minioUtils;


    @RequireRole(RoleConstant.DOCTOR)
    @Operation(summary = "上传影像文件")
    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择影像文件");
        }

        try {
            // 1. 调用你现有的工具类上传到 MinIO
            // 它会返回类似 http://127.0.0.1:9000/bucket/uuid_name.jpg 的 URL
            String fileUrl = minioUtils.upload(file,"doctor/images/");

            // 2. 从 URL 中提取或直接使用这个 URL 作为任务标识
            // 建议：为了让 Python 更好处理，我们可以生成一个 taskId
            String taskId = UUID.randomUUID().toString().substring(0, 8);

            // 3. 发送给 RabbitMQ
            // 这里传给 Python 的可以是整个 fileUrl，Python 直接 requests.get(fileUrl) 就能拿到图
            diagnosisService.sendToAI(taskId, fileUrl);

            return Result.success(taskId);

        } catch (Exception e) {
            return Result.error("上传及诊断请求失败: " + e.getMessage());
        }
    }

}
