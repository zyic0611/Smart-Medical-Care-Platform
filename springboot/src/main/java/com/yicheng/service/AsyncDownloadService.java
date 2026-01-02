package com.yicheng.service;


import com.yicheng.entity.MedicalImaging;
import com.yicheng.mapper.ImageMapper;
import com.yicheng.model.DownloadTaskInfo;
import com.yicheng.utils.MultiThreadDownloader;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//异步下载
@Service
public class AsyncDownloadService {

    @Resource
    private ImageMapper imageMapper;

    @Resource
    private MultiThreadDownloader multiThreadDownloader;

    // 内存数据库：存放所有下载任务的状态 (Key: taskId, Value: TaskInfo)
    private final Map<String, DownloadTaskInfo> taskMap = new ConcurrentHashMap<>();

    // 1. 开始任务接口
    public String startAsyncDownload(Integer imageId) {
        // 生成一个唯一的任务ID
        String taskId = UUID.randomUUID().toString();

        // 初始化任务信息
        DownloadTaskInfo taskInfo = new DownloadTaskInfo();
        taskInfo.setTaskId(taskId);
        taskInfo.setStatus("PENDING");
        taskMap.put(taskId, taskInfo);

        // --- 核心：开启一个新线程去跑下载，主线程立刻返回 taskId ---
        new Thread(() -> {
            executeDownload(taskId, imageId);
        }).start();

        return taskId; // 这里立刻返回，前端不会超时
    }


    // 后台具体执行下载的方法
    private void executeDownload(String taskId, Integer imageId) {
        DownloadTaskInfo task = taskMap.get(taskId);
        try {
            task.setStatus("RUNNING");

            // 查询数据库
            MedicalImaging image = imageMapper.selectById(imageId);
            if (image == null) throw new RuntimeException("图片不存在");

            task.setFileName(image.getFileName());

            // 设置路径
            String tempDir = System.getProperty("java.io.tmpdir");
            String localPath = tempDir + File.separator + image.getFileName();
            File localFile = new File(localPath);

            // 缓存检查
            if (localFile.exists() && localFile.length() > 0) {
                System.out.println("命中缓存");
            } else {
                // 执行耗时的下载
                // 注意：这里调用你的下载器
                multiThreadDownloader.download(image.getFileUrl(), localPath, 5);
            }

            // 下载完成，更新状态
            task.setFilePath(localPath);
            task.setStatus("COMPLETED");
            task.setProgress(100);
            System.out.println("任务 " + taskId + " 完成");

        } catch (Exception e) {
            e.printStackTrace();
            task.setStatus("FAILED");
            task.setErrorMsg(e.getMessage());
        }
    }


    // 查询状态接口
    public DownloadTaskInfo getTaskStatus(String taskId) {
        return taskMap.get(taskId);
    }

    // 获取文件路径接口 (用于最后下载)
    public File getCompletedFile(String taskId) {
        DownloadTaskInfo task = taskMap.get(taskId);
        if (task == null || !"COMPLETED".equals(task.getStatus())) {
            throw new RuntimeException("文件未准备好或任务不存在");
        }
        return new File(task.getFilePath());
    }
}
