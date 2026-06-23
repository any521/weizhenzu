package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券类型
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
public enum CouponType {

    FULL_REDUCTION(1, "满减"),
    DISCOUNT(2, "折扣"),
    NO_THRESHOLD(3, "无门槛");

    private final Integer code;
    private final String desc;

    CouponType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CouponType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
