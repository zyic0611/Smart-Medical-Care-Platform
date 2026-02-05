package com.yicheng.modules.appointment.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment")
public class appointmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long doctorId;
    private Long elderId;

    private LocalDateTime createTime;

    // 1-成功, 2-取消
    private Integer status;


}
