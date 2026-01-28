package com.yicheng.modules.elder.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ElderlyDTO {
    @NotBlank(message = "老人姓名不能为空")
    private String name;

    private String gender;

    @Min(value = 0, message = "年龄不能小于0")
    private Integer age;

    private String healthStatus;
    private LocalDate checkInDate;
    private Integer nurseId;
    private Integer bedId;
}