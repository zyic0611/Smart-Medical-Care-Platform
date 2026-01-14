package com.yicheng.modules.medicalimg.controller;


import com.yicheng.common.RequireRole;
import com.yicheng.common.Result;
import com.yicheng.common.RoleConstant;
import com.yicheng.modules.medicalimg.entity.MedicalImaging;
import com.yicheng.modules.medicalimg.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("api/imaging")
@Tag(name="老人影像管理模块")
public class ImageController {

    @Resource
    private  ImageService imageService;

    // 新增影像记录
    @PostMapping("/add")
    @RequireRole(RoleConstant.DOCTOR)
    @Operation(summary = "新增影像记录")
    public Result add(@RequestBody MedicalImaging medicalImaging) {
        // 设个默认时间
        medicalImaging.setCreateTime(new Date());
        imageService.add(medicalImaging);
        return Result.success();
    }

    @GetMapping("selectByElderId")
    @Operation(summary = "根据老人ID查询其影像记录")
    public Result<List<MedicalImaging>>selectByElderId(@RequestParam("elderId")Integer id){
        return Result.success(imageService.selectByElderId(id));
    }


    @GetMapping("download")
    @Operation(summary = "下载医学影像")
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
