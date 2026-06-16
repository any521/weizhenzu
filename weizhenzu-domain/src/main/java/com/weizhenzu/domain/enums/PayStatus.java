package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态枚举
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum PayStatus {

    UNPAID(0, "未支付"),
    PAID(1, "已支付"),
    REFUNDING(2, "退款中"),
    REFUNDED(3, "已退款");

    private final Integer code;
    private final String desc;

    public static PayStatus of(Integer code) {
        for (PayStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("非法支付状态: " + code);
    }
}
