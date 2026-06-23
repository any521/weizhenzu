package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 订单状态枚举（含状态机）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
public enum OrderStatus {

    PENDING_PAY(0, "待支付"),
    PAID(1, "待接单"),
    MERCHANT_ACCEPTED(2, "商家已接单"),
    RIDER_TAKEN(3, "骑手已接单"),
    PICKED_UP(4, "已取餐"),
    DELIVERING(5, "配送中"),
    DELIVERED(6, "已送达"),
    COMPLETED(7, "已完成"),
    CANCELED(8, "已取消"),
    REFUNDING(9, "退款中"),
    REFUNDED(10, "已退款");

    private final Integer code;
    private final String desc;

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus of(Integer code) {
        for (OrderStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("非法订单状态: " + code);
    }

    /**
     * 合法的状态转移
     */
    public Set<OrderStatus> nextAllowed() {
        return switch (this) {
            case PENDING_PAY -> EnumSet.of(PAID, CANCELED);
            case PAID -> EnumSet.of(MERCHANT_ACCEPTED, CANCELED, REFUNDING);
            case MERCHANT_ACCEPTED -> EnumSet.of(RIDER_TAKEN, CANCELED, REFUNDING);
            case RIDER_TAKEN -> EnumSet.of(PICKED_UP, CANCELED);
            case PICKED_UP -> EnumSet.of(DELIVERING);
            case DELIVERING -> EnumSet.of(DELIVERED);
            case DELIVERED -> EnumSet.of(COMPLETED);
            case COMPLETED -> EnumSet.of(REFUNDING);
            case REFUNDING -> EnumSet.of(REFUNDED, COMPLETED);
            case CANCELED, REFUNDED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitTo(OrderStatus target) {
        return nextAllowed().contains(target);
    }
}
