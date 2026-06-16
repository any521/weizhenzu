package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款信息 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "退款信息")
public class RefundVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "退款ID")
    private Long id;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "退款金额")
    private BigDecimal amount;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "退款状态：0申请中 1商家同意 2商家拒绝 3平台介入 4退款中 5已退款 6已取消")
    private Integer status;

    @Schema(description = "退款状态描述")
    private String statusDesc;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "退款时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
