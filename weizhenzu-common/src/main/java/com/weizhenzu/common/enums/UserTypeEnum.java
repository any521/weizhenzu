package com.weizhenzu.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型枚举
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UserTypeEnum {

    USER(1, "C端用户"),
    MERCHANT(2, "商家"),
    RIDER(3, "骑手"),
    ADMIN(4, "管理员");

    private final Integer code;
    private final String desc;

    public static UserTypeEnum of(Integer code) {
        for (UserTypeEnum t : values()) {
            if (t.code.equals(code)) return t;
        }
        throw new IllegalArgumentException("非法用户类型: " + code);
    }
}
