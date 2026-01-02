package com.yicheng.model;


import lombok.Data;

@Data
public class DownloadTaskInfo {
    private String taskId;      // 任务唯一ID
    private String status;      // PENDING(等待), RUNNING(下载中), COMPLETED(完成), FAILED(失败)
    private String fileName;    // 文件名
    private String filePath;    // 下载完成后保存在服务器的本地路径
    private String errorMsg;    // 错误信息
    private int progress;       // 进度 (0-100，可选，需要改造下载器支持)
}
