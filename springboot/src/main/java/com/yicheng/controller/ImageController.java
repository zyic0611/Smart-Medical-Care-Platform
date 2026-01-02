package com.yicheng.controller;


import com.yicheng.common.Result;
import com.yicheng.entity.MedicalImaging;
import com.yicheng.service.ImageService;
import com.yicheng.utils.MultiThreadDownloader;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("api/imaging")
public class ImageController {

    @Resource
    private  ImageService imageService;

    // 新增影像记录
    @PostMapping("/add")
    public Result add(@RequestBody MedicalImaging medicalImaging) {
        // 设个默认时间
        medicalImaging.setCreateTime(new Date());
        imageService.add(medicalImaging);
        return Result.success();
    }

    @GetMapping("selectByElderId")
    public Result<List<MedicalImaging>>selectByElderId(@RequestParam("elderId")Integer id){
        return Result.success(imageService.selectByElderId(id));
    }


    @GetMapping("download")
    public void download(@RequestParam("id")Integer id, HttpServletResponse response){
        try {
            // 1. 调用 Service 拿到已经下载到服务器本地的文件
            File file = imageService.downloadFile(id);

            // 2. 设置响应头，告诉浏览器“这是一个要下载的文件”，而不是直接在页面显示

            //防止之前可能有一些过滤器（Filter）或者拦截器往响应里塞了别的东西（比如空格、换行符等）。这一步是为了保证我们要发的文件流是纯净的，没有杂质。
            response.reset();

            //告诉浏览器，这里面是一堆二进制数据流
            response.setContentType("application/octet-stream");


            response.setCharacterEncoding("utf-8");
            // 处理中文文件名乱码问题
            //把中文名字翻译成浏览器能看懂的代码
            String encodedFileName = URLEncoder.encode(file.getName(), "UTF-8").replaceAll("\\+", "%20");

            //附件 Attachment：浏览器会立刻弹出一个“保存文件”的对话框。
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            // 3. 将服务器本地文件读取流 -> 写入到 Response 输出流 -> 发送给浏览器
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {

                byte[] buffer = new byte[8192]; // 8KB 缓冲区
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 如果出错，给前端返回一个错误信息
            try {
                response.setStatus(500);
                response.getWriter().write("Download Error: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }


}
