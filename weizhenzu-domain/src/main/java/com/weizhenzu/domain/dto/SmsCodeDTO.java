package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 发送短信验证码 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "发送短信验证码请求")
public class SmsCodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "场景：LOGIN/REGISTER/RESET", example = "LOGIN")
    @NotBlank(message = "场景不能为空")
    private String scene;
}
