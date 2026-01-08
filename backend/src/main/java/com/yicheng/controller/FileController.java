package com.yicheng.controller;

import com.yicheng.common.Result;
import com.yicheng.utils.MinioUtils; // 1. 引入刚才写的工具类
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileController {

    // 2. 注入 Minio 工具类 (不再需要定义 ROOT_PATH 了)
    @Resource
    private MinioUtils minioUtils;

    /**
     * 文件上传接口
     * 前端 (el-upload) 会调用这个接口
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        try {
            // 3. 一行代码调用工具类，上传到 MinIO
            // 工具类会自动处理：生成唯一文件名 -> 上传 -> 拼接 URL
            String url = minioUtils.upload(file);

            // 4. 直接返回云端 URL 给前端
            // 比如：http://localhost:9000/yicheng-medical/xxxx-avatar.png
            return Result.success(url);

        } catch (Exception e) {
            // 5. 如果上传失败，打印错误日志，并告诉前端失败原因
            e.printStackTrace();
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}