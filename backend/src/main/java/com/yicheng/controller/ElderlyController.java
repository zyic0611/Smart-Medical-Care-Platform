package com.yicheng.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yicheng.common.AutoLog;
import com.yicheng.common.RequireAdmin;
import com.yicheng.common.Result;
import com.yicheng.entity.Elderly;
import com.yicheng.service.ElderlyService;
import com.yicheng.service.ElderlyServiceIml;
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
    @RequireAdmin
    @PutMapping("/update")
    @Operation(summary = "更新老人信息")
    public Result<?> update(@RequestBody Elderly elderly){
        elderlyService.updateElderlyWithBed(elderly);
        return Result.success();
    }


    @AutoLog("新增老人")
    @RequireAdmin
    @PostMapping("/add")
    @Operation(summary = "新增老人")
    public Result<?> add(@RequestBody Elderly elderly){
        elderlyService.addElderlyWithBed(elderly);
        return Result.success();
    }


    @AutoLog("删除老人")
    @RequireAdmin
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除老人")
    public Result<?> delete(@PathVariable Integer id){

        elderlyService.deleteById(id);
        return Result.success();
    }

}
