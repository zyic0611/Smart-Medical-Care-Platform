package com.yicheng.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MobileLoginRequest {
    @Schema(description = "手机号", example = "15060308341")
    private String phone;
    @Schema(description = "验证码", example = "123456")
    private String code;
}