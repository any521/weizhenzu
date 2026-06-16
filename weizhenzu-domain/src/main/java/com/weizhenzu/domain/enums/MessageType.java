package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MessageType {

    ORDER(1, "订单"),
    PROMOTION(2, "优惠"),
    SYSTEM(3, "系统"),
    SERVICE(4, "客服");

    private final Integer code;
    private final String desc;

    public static MessageType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (MessageType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
