package com.yicheng.modules.elder.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.yicheng.common.AutoLog;
import com.yicheng.common.RequireRole;
import com.yicheng.common.Result;
import com.yicheng.common.RoleConstant;
import com.yicheng.modules.elder.pojo.dto.ElderlyDTO;
import com.yicheng.modules.elder.pojo.dto.ElderlyUpdateDTO;
import com.yicheng.modules.elder.pojo.vo.ElderlyVO;
import com.yicheng.modules.elder.service.IElderlyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/elderly")
@Tag(name="老人管理模块")
@RequiredArgsConstructor
public class ElderlyController {

    private final IElderlyService IElderlyService;

    /**
     * 分页查询接口
     */

    @GetMapping("/page")
    @Operation(summary = "分页查询老人")
    @ApiOperationSupport(order = 1)
    public Result<IPage<ElderlyVO>> selectPage(
            ElderlyVO elderlyVO,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {

        IPage<ElderlyVO> pageResult = IElderlyService.selectElderlyPage(pageNum, pageSize, elderlyVO);

        return Result.success(pageResult);
    }


    @AutoLog("更新老人")
    @RequireRole(RoleConstant.ADMIN)
    @PutMapping("/update")
    @Operation(summary = "更新老人信息")
    @ApiOperationSupport(order = 2)
    public Result<Boolean> update(@RequestBody ElderlyUpdateDTO elderlyUpdateDTO){

        return Result.success(IElderlyService.updateElderlyWithBed(elderlyUpdateDTO));
    }


    @AutoLog("新增老人")
    @RequireRole(RoleConstant.ADMIN)
    @PostMapping("/add")
    @Operation(summary = "新增老人")
    @ApiOperationSupport(order = 3)
    public Result<Boolean> add(@RequestBody ElderlyDTO elderlyDTO){

        return Result.success(IElderlyService.addElderlyWithBed(elderlyDTO));
    }



    @AutoLog("批量删除老人")
    @RequireRole(RoleConstant.ADMIN)
    @PostMapping("/remove")
    @Operation(summary = "删除老人",description = "传入逗号分隔的id字符串")
    @ApiOperationSupport(order = 4)
    public Result<Boolean> remove(@Parameter(description = "主键集合",required = true) @RequestParam String ids){


        return Result.success(IElderlyService.deleteLogic(ids));
    }


    @GetMapping("/pagewithimages")
    @Operation(summary = "分页查询有影像的老人")
    @ApiOperationSupport(order = 5)
    public Result<IPage<ElderlyVO>> getElderlyWithImages(@RequestParam(defaultValue = "1") Integer pageNum,  // 默认第一页
                                                             @RequestParam(defaultValue = "10") Integer pageSize)  // 默认每页10条)
    {
        return Result.success(IElderlyService.listElderlyWithImagingByPage(pageNum, pageSize));
    }

}
