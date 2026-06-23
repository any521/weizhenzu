package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户优惠券状态
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
public enum UserCouponStatus {

    UNUSED(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期");

    private final Integer code;
    private final String desc;

    UserCouponStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserCouponStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserCouponStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
