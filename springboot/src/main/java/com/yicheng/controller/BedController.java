package com.yicheng.controller;


import com.github.pagehelper.PageInfo;
import com.yicheng.common.AutoLog;
import com.yicheng.common.RequireAdmin;
import com.yicheng.common.Result;
import com.yicheng.entity.Bed;
import com.yicheng.entity.SysUser;
import com.yicheng.mapper.BedMapper;
import com.yicheng.service.BedService;
import com.yicheng.utils.JwtUtils;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bed")
public class BedController {


    @Resource
    private BedService bedService;




    // 改成查空闲床位的方法
    @GetMapping("/list")
    public Result<List<Bed>> list() {
        List<Bed> list = bedService.selectFreeBeds();
        return Result.success(list);
    }


    //分页查找
    @GetMapping("/page")
    public Result<PageInfo<Bed>> selectPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String bedNumber
    ) {
        PageInfo<Bed> pageInfo = bedService.selectPage(pageNum, pageSize, bedNumber);
        return Result.success(pageInfo);
    }

    //新增接口
    @AutoLog("新增床位")
    @RequireAdmin
    @PostMapping("/add")
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
