package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付记录状态（与 PayStatus 不同，这里是支付单状态）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
public enum PaymentStatus {

    PENDING(0, "待支付"),
    SUCCESS(1, "成功"),
    FAIL(2, "失败"),
    CLOSED(3, "已关闭"),
    REFUNDED(4, "已退款");

    private final Integer code;
    private final String desc;

    PaymentStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PaymentStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
