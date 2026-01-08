package com.yicheng.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class FileCleanupTask {
    // 每隔 1 小时执行一次 (单位：毫秒)
    //@Scheduled(fixedRate = 3600000)
    public void cleanupTempFiles() {
        System.out.println("🧹 [定时任务] 开始清理过期的临时影像文件...");

        String tempDir = System.getProperty("java.io.tmpdir");
        File dir = new File(tempDir);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                long now = System.currentTimeMillis();
                // 设定过期时间：比如只保留最近 30 分钟的文件
                // 30 * 60 * 1000 = 1800000
                long expireTime = 1800000;

                for (File file : files) {
                    // 假设你的影像文件都有特定的后缀，比如 .raw 或 .dicom，或者是你下载时生成的特定名字
                    // 加上过滤条件，防止误删系统其他临时文件！！
                    // 比如：if (file.getName().endsWith(".raw") || file.getName().contains("image_"))

                    if (file.isFile() && (now - file.lastModified() > expireTime)) {
                        try {
                            // 这里建议加上文件名特征判断，避免误删系统文件
                            if(file.getName().contains(".")){ // 简单的防御性判断
                                boolean deleted = file.delete();
                                if (deleted) {
                                    System.out.println("   已删除过期文件: " + file.getName());
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("   删除失败: " + file.getName());
                        }
                    }
                }
            }
        }
    }
}
