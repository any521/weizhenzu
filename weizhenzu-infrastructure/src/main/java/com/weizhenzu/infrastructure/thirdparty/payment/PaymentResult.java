package com.weizhenzu.infrastructure.thirdparty.payment;

import lombok.Data;

import java.io.Serializable;

/**
 * 支付结果
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
public class PaymentResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;
    private String payUrl;
    private String thirdPartyNo;
    private Boolean paid;
    private Boolean mock;
    private String errorMsg;
}
