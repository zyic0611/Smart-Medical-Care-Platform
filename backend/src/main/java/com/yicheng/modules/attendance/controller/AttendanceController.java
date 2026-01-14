package com.yicheng.modules.attendance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yicheng.common.Result;
import com.yicheng.modules.attendance.entity.AttendanceRecord;
import com.yicheng.modules.attendance.service.AttendanceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name="打卡模块")
@RequestMapping("/attendance")
public class AttendanceController {
    @Autowired
    @Resource
    private AttendanceRecordService attendanceRecordService;

    @Operation(summary = "打卡接口")
    @PostMapping("/punch")
    public Result<String> punch(){
        String msg=attendanceRecordService.processPunch();
        return Result.success(msg);
    }


    @Operation(summary="查询最近15条接口")
    @PostMapping("/resent")
    public Result<IPage<AttendanceRecord>> getResent(){
        //固定就查15条 1页
        IPage<AttendanceRecord> pageData= attendanceRecordService.getRecentRecordsPage(1,15);

        return Result.success(pageData);


    }
}
