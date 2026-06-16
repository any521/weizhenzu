package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作人类型
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum OperatorType {

    USER(1, "用户"),
    MERCHANT(2, "商家"),
    RIDER(3, "骑手"),
    ADMIN(4, "管理员"),
    SYSTEM(5, "系统");

    private final Integer code;
    private final String desc;

    public static OperatorType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OperatorType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
