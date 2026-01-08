package com.yicheng.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yicheng.common.AutoLog;
import com.yicheng.common.RequireAdmin;
import com.yicheng.common.Result;
import com.yicheng.entity.Bed;
import com.yicheng.entity.SysUser;
import com.yicheng.service.BedService;
import com.yicheng.service.BedServiceIml;
import com.yicheng.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bed")
@Tag(name="床位管理模块")
public class BedController {


    @Resource
    private BedService bedService;




    // 改成查空闲床位的方法
    @GetMapping("/list")
    @Operation(summary = "查询空闲床位")
    public Result<List<Bed>> list() {
        List<Bed> list = bedService.selectFreeBeds();
        return Result.success(list);
    }


    //分页查找
    @GetMapping("/page")
    @Operation(summary = "分页查询床位")
    public Result<IPage<Bed>> selectPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String bedNumber
    ) {
        IPage<Bed> pageInfo = bedService.selectPage(pageNum, pageSize, bedNumber);
        return Result.success(pageInfo);
    }

    //新增接口
    @AutoLog("新增床位")
    @RequireAdmin
    @PostMapping("/add")
    @Operation(summary = "新增床位")
    public Result<?> add(@RequestBody Bed bed) {

        // 1. 查一下是谁在操作
        SysUser currentUser = JwtUtils.getCurrentUser();

        // 2. 检查权限
        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("403", "无权操作，请联系管理员");
        }

        bedService.add(bed);
        return Result.success();
    }

    //更新接口
    @AutoLog("更新床位")
    @Operation(summary = "更新床位")
    @RequireAdmin
    @PutMapping("/update")
    public Result<?> update(@RequestBody Bed bed) {

        // 1. 查一下是谁在操作
        SysUser currentUser = JwtUtils.getCurrentUser();

        // 2. 检查权限
        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("403", "无权操作，请联系管理员");
        }


        bedService.update(bed);
        return Result.success();
    }

    //删除接口
    @AutoLog("删除床位")
    @RequireAdmin
    @Operation(summary = "删除床位")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Integer id) {


        // 1. 查一下是谁在操作
        SysUser currentUser = JwtUtils.getCurrentUser();

        // 2. 检查权限
        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("403", "无权操作，请联系管理员");
        }


        bedService.delete(id);
        return Result.success();
    }


}
