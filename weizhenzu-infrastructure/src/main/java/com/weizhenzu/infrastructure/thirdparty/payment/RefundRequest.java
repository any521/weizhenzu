package com.weizhenzu.infrastructure.thirdparty.payment;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 退款请求
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
public class RefundRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String paymentNo;
    private String refundNo;
    private BigDecimal amount;
    private String reason;
}
