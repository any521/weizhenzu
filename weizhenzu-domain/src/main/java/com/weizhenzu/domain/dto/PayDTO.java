package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付请求 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "支付请求")
public class PayDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "支付方式：1支付宝 2微信 3余额")
    @NotNull(message = "支付方式不能为空")
    private Integer payType;

    @Schema(description = "客户端：APP/WEB/MINI")
    private String client;
}
