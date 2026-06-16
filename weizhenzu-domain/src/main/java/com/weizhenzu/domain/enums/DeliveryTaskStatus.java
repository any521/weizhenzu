package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 配送任务状态枚举
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum DeliveryTaskStatus {

    PENDING_GRAB(0, "待抢"),
    GRABBED(1, "已抢"),
    ARRIVED_MERCHANT(2, "到店"),
    PICKED_UP(3, "取餐"),
    DELIVERING(4, "配送中"),
    DELIVERED(5, "已送达"),
    CANCELED(6, "已取消");

    private final Integer code;
    private final String desc;

    public static DeliveryTaskStatus of(Integer code) {
        for (DeliveryTaskStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("非法配送任务状态: " + code);
    }
}
