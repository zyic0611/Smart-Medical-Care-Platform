package com.yicheng.modules.appointment.pojo.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yicheng.modules.appointment.pojo.entity.appointmentEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
public class appointmentVO  {

    @JsonIgnore
    private Long doctorId;
    @JsonIgnore
    private Long elderId;



    private LocalDateTime createTime;

    private LocalDateTime payTime;

    // 1-成功, 2-取消
    @JsonIgnore
    private Integer status;

    private String orderSn;


    String statusStr;
    String doctorName;
}
