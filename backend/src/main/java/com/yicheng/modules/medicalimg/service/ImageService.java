package com.yicheng.modules.medicalimg.service;


import com.yicheng.modules.medicalimg.entity.MedicalImaging;
import com.yicheng.modules.medicalimg.mapper.ImageMapper;
import com.yicheng.utils.MultiThreadDownloader;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class ImageService {
    @Resource
    private ImageMapper imageMapper;

    @Resource
    private MultiThreadDownloader  multiThreadDownloader;


    public List<MedicalImaging> selectByElderId(Integer id){
        return imageMapper.selectByElderId(id);
    }
    /*
    下载文件业务逻辑：
    1.查数据库拿到URL
    2.启动多线程
    3/返回下载好的本地对象
    * */
    public File downloadFile(Integer id) throws Exception {

        // 1. 根据ID获取图像对象
        MedicalImaging image = imageMapper.selectById(id);
        if (image == null) {
            throw new RuntimeException("医学影像不存在，ID: " + id);
        }

        // 关键校验：确保数据库里存的下载地址不为空
        String targetUrl = image.getFileUrl();
        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            throw new RuntimeException("该影像记录没有有效的下载地址(file_url)，ID: " + id);
        }

        // 2. 准备临时路径 (系统临时目录 + 文件名)
        String tempDir = System.getProperty("java.io.tmpdir");
        // 建议：如果文件名可能重复，可以考虑用 ID + 文件名: id + "_" + image.getFileName()
        String localPath = tempDir + File.separator + image.getFileName();
        File localFile = new File(localPath);

        // 3. 性能优化：缓存检查
        // 如果服务器临时目录已经有这个文件了，且大小大于0，直接返回
        if (localFile.exists() && localFile.length() > 0) {
            System.out.println("命中服务器缓存，无需重复下载，路径：" + localPath);
            return localFile;
        }

        // 4. 准备下载
        System.out.println("开始从第三方存储多线程加速下载，URL: " + targetUrl);

        try {
            // 5. 开启5个线程下载
            // 【关键修改】：第一个参数必须传 URL (targetUrl)，而不是文件名
            // 假设你的 download 方法签名是: download(String url, String localPath, int threadCount)
            multiThreadDownloader.download(targetUrl, localPath, 5);

        } catch (Exception e) {
            // 如果下载过程报错，最好把可能产生的半成品文件删掉，避免下次误判为缓存命中
            if (localFile.exists()) {
                localFile.delete();
            }
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }

        // [新增] 双重确认：确保下载器执行完后，文件真的存在
        if (!localFile.exists() || localFile.length() == 0) {
            throw new RuntimeException("下载任务结束，但文件未找到或大小为0");
        }

        return localFile;
    }

    public void add(MedicalImaging medicalImaging) {
        imageMapper.insert(medicalImaging);
    }
}
