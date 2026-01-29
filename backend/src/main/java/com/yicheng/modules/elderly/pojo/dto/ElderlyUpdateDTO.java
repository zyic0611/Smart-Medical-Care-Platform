package com.yicheng.modules.elderly.pojo.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class ElderlyUpdateDTO extends ElderlyDTO {

    @NotNull(message = "修改时ID不能为空")
    private Integer id; // 修改必须带 ID
}
