package com.yicheng.modules.attendance.entity;

import cn.hutool.core.date.DateTime;
import lombok.Data;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.io.Serializable;

/**
 * (AttendanceRecord)实体类
 *
 * @author makejava
 * @since 2026-01-14 12:45:47
 */
@Data
public class AttendanceRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = -52324462728654010L;

    private Long id;
/**
     * 用户ID
     */
    private Integer userId;
/**
     * 关联员工ID
     */
    private Integer linkId;
/**
     * 上班时间
     */
    private LocalDateTime checkInTime;
/**
     * 下班时间
     */
    private LocalDateTime checkOutTime;
/**
     * 工作日期
     */
    private LocalDate workDate;


}

