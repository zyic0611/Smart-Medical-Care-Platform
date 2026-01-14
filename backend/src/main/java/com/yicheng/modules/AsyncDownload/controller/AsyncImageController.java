package com.yicheng.modules.AsyncDownload.controller;


import com.yicheng.common.Result;
import com.yicheng.model.DownloadTaskInfo;
import com.yicheng.modules.AsyncDownload.service.AsyncDownloadService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/async-download")
public class AsyncImageController {
    @Resource
    private AsyncDownloadService asyncDownloadService;

    // 接口1：触发下载
    // 返回值：{"taskId": "abc-123-xyz", "message": "开始下载..."}
    @GetMapping("/start/{id}")
    public Result<Map<String, String>> startDownload(@PathVariable Integer id) {
        String taskId = asyncDownloadService.startAsyncDownload(id);

        Map<String, String> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("message", "服务器已开始加速下载，请通过taskId查询进度");
        return Result.success(result);
    }

    // 接口2：轮询状态
    // 前端每 2 秒调用一次
    @GetMapping("/status/{taskId}")
    public Result<DownloadTaskInfo> checkStatus(@PathVariable String taskId) {
        return Result.success(asyncDownloadService.getTaskStatus(taskId));
    }

    // 接口3：最终取文件 (只有状态变成 COMPLETED 才调用)
    @GetMapping("/fetch/{taskId}")
    public void fetchFile(@PathVariable String taskId, HttpServletResponse response) throws IOException {
        File file = asyncDownloadService.getCompletedFile(taskId);

        // 设置响应头，告诉浏览器这是个文件
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(file.getName(), "UTF-8"));
        response.setContentLengthLong(file.length());

        // 使用流传输文件
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096]; // 4KB 缓冲
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }
}
