package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 密码登录 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "密码登录请求")
public class PasswordLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "密码", example = "password123")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度6-32位")
    private String password;

    @Schema(description = "用户类型：1用户 2商家 3骑手 4管理员", example = "1")
    private Integer userType;
}
