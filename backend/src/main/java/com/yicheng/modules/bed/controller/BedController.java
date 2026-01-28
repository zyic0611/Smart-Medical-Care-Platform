package com.yicheng.modules.bed.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.yicheng.common.AutoLog;
import com.yicheng.common.RequireRole;
import com.yicheng.common.Result;
import com.yicheng.common.RoleConstant;
import com.yicheng.modules.bed.pojo.dto.BedAddDTO;
import com.yicheng.modules.bed.pojo.dto.BedUpdateDTO;
import com.yicheng.modules.bed.pojo.vo.BedVO;
import com.yicheng.modules.bed.service.IBedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@AllArgsConstructor
@RestController
@RequestMapping("/bed")
@Tag(name="床位管理模块")
public class BedController {


    private final IBedService IBedService;

    /**
     * 查询空闲床位
     * */
    @GetMapping("/freelist")
    @Operation(summary = "查询空闲床位")
    @ApiOperationSupport(order = 1)
    public Result<List<BedVO>> list(BedVO bedVO) {
        return Result.success(IBedService.selectFreeBeds(bedVO));
    }




    //分页查找
    @GetMapping("/page")
    @Operation(summary = "分页查询床位")
    @ApiOperationSupport(order = 2)
    public Result<IPage<BedVO>> selectPage(
            BedVO bedVO,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        // 创建分页对象
        IPage<BedVO> page = new Page<>(current, pageSize);
        return Result.success(IBedService.selectBedPage(page,bedVO));
    }



    //新增接口
    @AutoLog("新增床位")
    @RequireRole(RoleConstant.ADMIN)
    @PostMapping("/add")
    @Operation(summary = "新增床位")
    public Result<Boolean> add(@Valid @RequestBody BedAddDTO bedAddDTO) {

        return Result.success(IBedService.save(bedAddDTO));
    }

    //更新接口
    @AutoLog("更新床位")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "更新床位")
    @RequireRole(RoleConstant.ADMIN)
    @PutMapping("/update")
    public Result<Boolean> update(@Valid@RequestBody BedUpdateDTO bedUpdateDTO) {


        return Result.success(IBedService.updateByDTO(bedUpdateDTO));
    }

    /**
     * 批量删除 (逻辑删除)
     */
    @AutoLog("批量删除床位")
    @RequireRole(RoleConstant.ADMIN)
    @PostMapping("/remove")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "删除床位", description = "传入逗号分隔的id字符串")
    public Result<Boolean> remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
        // 字符串转 Long 列表
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        // 调用 Service 的逻辑删除方法
        return Result.success(IBedService.deleteLogic(idList));
    }

    /**
     * 单个查询 使用redis缓存
     */
    @GetMapping("/detail/{id}")
    @ApiOperationSupport(order = 7)
    @Operation(summary = "查询单个床位")
    public Result<BedVO> detail(@PathVariable("id") Long id) {
        return Result.success(IBedService.detail(id));
    }
}
