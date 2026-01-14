package com.yicheng.modules.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.exception.CustomException;
import com.yicheng.modules.attendance.entity.AttendanceRecord;
import com.yicheng.modules.attendance.mapper.AttendanceRecordMapper;
import com.yicheng.modules.user.entity.SysUser;
import com.yicheng.utils.JwtUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AttendanceRecordServiceIml
        extends ServiceImpl<AttendanceRecordMapper, AttendanceRecord>
        implements AttendanceRecordService{

    @Override
    public String processPunch(){
        SysUser currentUser = JwtUtils.getCurrentUser();//获取当前的用户
        if(currentUser==null){
            throw new CustomException("401","登录信息已过期，请重新登录");
        }

        Integer userId = currentUser.getId();//获取用户表中的id
        Integer linkedID=currentUser.getLinkedId();//获取关链表中的id

        LocalDate today=LocalDate.now();//获取日期

        //查询今天打卡过没
        AttendanceRecord record=this.getOne(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getUserId, userId)
                .eq(AttendanceRecord::getWorkDate, today));

        if(record==null){
            AttendanceRecord newRecord=new AttendanceRecord();
            newRecord.setUserId(userId);
            newRecord.setLinkId(linkedID);
            newRecord.setWorkDate(today);//设置日期
            newRecord.setCheckInTime(LocalDateTime.now());//上班打卡
            this.save(newRecord);
            return "上班打卡成功！ 时间："+LocalDateTime.now().toLocalTime().withNano(0);
        }else{
            record.setCheckOutTime(LocalDateTime.now());
            this.updateById(record);
            return "下班打卡成功！ 时间："+LocalDateTime.now().toLocalTime().withNano(0);
        }
    }

    public IPage<AttendanceRecord> getRecentRecordsPage(int currentPage, int pageSize){
        Page<AttendanceRecord> page=new Page<>(currentPage,pageSize);//创建新的分页对象

        //执行分页查询
        return this.page(page,new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getUserId,JwtUtils.getCurrentUser().getId())
        .orderByDesc(AttendanceRecord::getWorkDate));

    }
}
