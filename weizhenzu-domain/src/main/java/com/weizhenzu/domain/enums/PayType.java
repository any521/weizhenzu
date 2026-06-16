package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式枚举
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum PayType {

    ALIPAY(1, "支付宝"),
    WECHAT(2, "微信支付"),
    BALANCE(3, "余额支付");

    private final Integer code;
    private final String desc;

    public static PayType of(Integer code) {
        for (PayType t : values()) {
            if (t.code.equals(code)) return t;
        }
        throw new IllegalArgumentException("非法支付方式: " + code);
    }
}
