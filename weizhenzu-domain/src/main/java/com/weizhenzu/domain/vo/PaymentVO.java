package com.weizhenzu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付信息 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "支付信息")
public class PaymentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付单号")
    private String paymentNo;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "支付金额")
    private BigDecimal amount;

    @Schema(description = "支付方式：1支付宝 2微信 3余额")
    private Integer payType;

    @Schema(description = "支付状态：0待支付 1成功 2失败 3已关闭 4已退款")
    private Integer status;

    @Schema(description = "支付URL（支付宝H5/微信扫码）")
    private String payUrl;

    @Schema(description = "第三方交易号")
    private String thirdPartyNo;
}
