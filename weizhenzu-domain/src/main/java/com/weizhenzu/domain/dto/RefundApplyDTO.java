package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 申请退款 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "申请退款请求")
public class RefundApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "退款原因")
    @NotBlank(message = "退款原因不能为空")
    private String reason;

    @Schema(description = "退款金额")
    @NotNull(message = "退款金额不能为空")
    private BigDecimal amount;

    @Schema(description = "图片凭证")
    private List<String> images;
}
