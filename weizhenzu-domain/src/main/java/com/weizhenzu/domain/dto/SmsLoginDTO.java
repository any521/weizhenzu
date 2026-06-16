package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 短信验证码登录 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "短信验证码登录请求")
public class SmsLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "验证码", example = "1234")
    @NotBlank(message = "验证码不能为空")
    private String code;

    @Schema(description = "用户类型：1用户 2商家 3骑手", example = "1")
    private Integer userType;
}
