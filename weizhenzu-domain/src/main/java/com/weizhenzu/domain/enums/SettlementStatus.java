package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算单状态
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
public enum SettlementStatus {

    PENDING(0, "待结算"),
    SETTLED(1, "已结算"),
    REJECTED(2, "已驳回");

    private final Integer code;
    private final String desc;

    SettlementStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SettlementStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (SettlementStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
