package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 刷新 Token DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "刷新Token请求")
public class RefreshTokenDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "刷新令牌", example = "eyJhbGciOi...")
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
