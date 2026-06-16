package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "修改密码请求")
public class PasswordUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原密码", example = "oldpass123")
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码", example = "newpass123")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度6-32位")
    private String newPassword;
}
