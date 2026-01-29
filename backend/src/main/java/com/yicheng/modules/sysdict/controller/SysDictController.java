package com.yicheng.modules.sysdict.controller;


import com.yicheng.common.Result;
import com.yicheng.modules.sysdict.pojo.entity.SysDict;
import com.yicheng.modules.sysdict.service.ISysDictService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name="系统字典",description = "系统字典")
@RequestMapping("/sys/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final ISysDictService sysDictService;

    /**
     * 前端下拉框通用接口
     * 访问：/sys/dict/list?code=gender
     */
    @GetMapping("/list")
    public Result<List<SysDict>> getListByCode(@RequestParam String code) {
        List<SysDict> list = sysDictService.listByCode(code);
        return Result.success(list);
    }

}
