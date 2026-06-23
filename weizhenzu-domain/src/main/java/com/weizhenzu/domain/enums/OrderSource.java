package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单来源
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
public enum OrderSource {

    APP(1, "APP"),
    MINI_PROGRAM(2, "小程序"),
    ADMIN(3, "后台");

    private final Integer code;
    private final String desc;

    OrderSource(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderSource of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderSource s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
