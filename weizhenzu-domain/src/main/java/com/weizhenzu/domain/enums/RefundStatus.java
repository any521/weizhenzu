package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 退款状态枚举
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum RefundStatus {

    APPLYING(0, "申请中"),
    MERCHANT_AGREED(1, "商家同意"),
    MERCHANT_REJECTED(2, "商家拒绝"),
    PLATFORM_INTERVENTION(3, "平台介入"),
    REFUNDING(4, "退款中"),
    REFUNDED(5, "已退款"),
    CANCELED(6, "已取消");

    private final Integer code;
    private final String desc;

    public static RefundStatus of(Integer code) {
        for (RefundStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("非法退款状态: " + code);
    }
}
