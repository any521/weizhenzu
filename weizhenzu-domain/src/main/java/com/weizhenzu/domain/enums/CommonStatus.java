package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态：0禁用 1正常
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CommonStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "正常");

    private final Integer code;
    private final String desc;

    public static CommonStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CommonStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
