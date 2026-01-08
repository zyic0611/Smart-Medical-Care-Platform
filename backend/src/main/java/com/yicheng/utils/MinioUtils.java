package com.yicheng.utils;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Component
public class MinioUtils {

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String secretKey;
    @Value("${minio.bucketName}")
    private String bucketName;

    private MinioClient minioClient;

    // 初始化客户端
    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 上传文件并返回访问 URL
     * @param file 前端传来的文件
     * @return 文件的网络访问路径
     */
    public String upload(MultipartFile file) {
        try {
            // 1. 获取文件流
            InputStream inputStream = file.getInputStream();

            // 2. 生成唯一文件名 (防止文件名冲突)
            // 比如: avatar.jpg -> 550e8400-e29b..._avatar.jpg
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + originalFilename;

            // 3. 上传到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType()) // 关键：设置类型，否则浏览器打开是下载而不是预览
                            .build()
            );

            // 4. 拼接返回 URL
            // 格式: http://localhost:9000/yicheng-medical/xxxx.jpg
            String url = endpoint + "/" + bucketName + "/" + fileName;
            return url;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("MinIO 上传失败: " + e.getMessage());
        }
    }
}