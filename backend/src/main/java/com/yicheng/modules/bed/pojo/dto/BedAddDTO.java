package com.yicheng.modules.bed.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class BedAddDTO implements Serializable {

    // ID 不需要，数据库自增
    // 状态 不需要，默认就是 0 (空闲)

    @Serial
    private static final long serialVersionUID = 1L;



    @NotBlank(message = "床位号不能为空")
    private String bedNumber;
}
