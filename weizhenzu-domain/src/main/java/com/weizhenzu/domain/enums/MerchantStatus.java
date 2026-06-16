package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商家状态
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MerchantStatus {

    PENDING_AUDIT(0, "待审核"),
    NORMAL(1, "正常"),
    DISABLED(2, "禁用"),
    REJECTED(3, "驳回");

    private final Integer code;
    private final String desc;

    public static MerchantStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (MerchantStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
