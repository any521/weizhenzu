package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券有效期类型
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CouponValidType {

    FIXED_TIME(1, "固定时间"),
    AFTER_RECEIVE(2, "领取后N天");

    private final Integer code;
    private final String desc;

    public static CouponValidType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponValidType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
