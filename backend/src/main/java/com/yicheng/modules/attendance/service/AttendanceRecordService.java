package com.yicheng.modules.attendance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.modules.attendance.entity.AttendanceRecord;

public interface AttendanceRecordService extends IService<AttendanceRecord> {
    String processPunch();

    IPage<AttendanceRecord> getRecentRecordsPage(int currentPage, int pageSize);

}
