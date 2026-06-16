package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券适用范围
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CouponScope {

    ALL(1, "全场"),
    SPECIFIED_MERCHANT(2, "指定商家"),
    SPECIFIED_CATEGORY(3, "指定类目");

    private final Integer code;
    private final String desc;

    public static CouponScope of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponScope s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
