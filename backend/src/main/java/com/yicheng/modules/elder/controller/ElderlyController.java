package com.yicheng.modules.elder.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yicheng.common.AutoLog;
import com.yicheng.common.RequireRole;
import com.yicheng.common.Result;
import com.yicheng.common.RoleConstant;
import com.yicheng.modules.elder.entity.Elderly;
import com.yicheng.modules.elder.service.ElderlyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/elderly")
@Tag(name="老人管理模块")
public class ElderlyController {

    @Resource
    private ElderlyService elderlyService;

    /**
     * 分页查询接口
     * URL: /elderly/page?pageNum=1&pageSize=10&name=张
     */


    @GetMapping("/page")
    @Operation(summary = "分页查询老人信息")
    public Result<IPage<Elderly>> selectPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name
    ) {
        // 👇 4. 这里的接收类型也变成 IPage
        IPage<Elderly> pageResult = elderlyService.selectPage(pageNum, pageSize, name);

        return Result.success(pageResult);
    }


    @AutoLog("更新老人")
    @RequireRole(RoleConstant.ADMIN)
    @PutMapping("/update")
    @Operation(summary = "更新老人信息")
    public Result<?> update(@RequestBody Elderly elderly){
        elderlyService.updateElderlyWithBed(elderly);
        return Result.success();
    }


    @AutoLog("新增老人")
    @RequireRole(RoleConstant.ADMIN)
    @PostMapping("/add")
    @Operation(summary = "新增老人")
    public Result<?> add(@RequestBody Elderly elderly){
        elderlyService.addElderlyWithBed(elderly);
        return Result.success();
    }


    @AutoLog("删除老人")
    @RequireRole(RoleConstant.ADMIN)
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除老人")
    public Result<?> delete(@PathVariable Integer id){

        elderlyService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/pagewithimages")
    @Operation(summary = "查询有影像的老人")
    public Result<IPage<Elderly>> getElderlyWithImages(@RequestParam(defaultValue = "1") Integer pageNum,  // 默认第一页
                                                       @RequestParam(defaultValue = "10") Integer pageSize)  // 默认每页10条)
    {
        try {
            IPage<Elderly> elderPage = elderlyService.listElderlyWithImagingByPage(pageNum, pageSize);
            return Result.success(elderPage);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("分页查询有影像的老人列表失败：" + e.getMessage());
        }
    }

}
