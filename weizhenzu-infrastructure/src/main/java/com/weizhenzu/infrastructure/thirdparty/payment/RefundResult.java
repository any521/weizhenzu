package com.weizhenzu.infrastructure.thirdparty.payment;

import lombok.Data;

import java.io.Serializable;

/**
 * 退款结果
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
public class RefundResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;
    private String thirdPartyNo;
    private Boolean mock;
    private String errorMsg;
}
