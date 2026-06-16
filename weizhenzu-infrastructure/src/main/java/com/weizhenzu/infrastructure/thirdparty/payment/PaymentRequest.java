package com.weizhenzu.infrastructure.thirdparty.payment;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付请求
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
public class PaymentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String paymentNo;
    private String orderNo;
    private BigDecimal amount;
    private String subject;
    private String userId;
}
