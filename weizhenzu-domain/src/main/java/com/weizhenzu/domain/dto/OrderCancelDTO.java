package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 取消订单 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "取消订单请求")
public class OrderCancelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "取消原因")
    @NotBlank(message = "取消原因不能为空")
    private String reason;
}
