package com.weizhenzu.domain.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 订单状态枚举（含状态机）
 *
 * <p>标准外卖流程（商家备餐与骑手赶往商家并行）：
 * 待支付(0) → 待接单(1) → 备餐中(2，商家接单后立即推送骑手抢单) → 骑手已接单(3) → 骑手已到店(11) → 配送中(5) → 已送达(6) → 已完成(7)
 *
 * <p>堂食流程：
 * 待支付(0) → 待接单(1) → 备餐中(2) → 已送达/待取餐(6) → 已完成(7)
 *
 * <p>说明：
 * - 商家接单(2)时立即创建配送任务并广播骑手抢单，商家备餐与骑手赶往商家并行进行
 * - 商家出餐(merchantReady)仅发送通知给已接单骑手，不改变订单主状态
 * - 骑手取餐时订单状态变为配送中(5)
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
public enum OrderStatus {

    PENDING_PAY(0, "待支付"),
    PENDING_ACCEPT(1, "待接单"),
    PREPARING(2, "备餐中"),
    RIDER_ACCEPTED(3, "骑手已接单"),
    // 4号状态码已废弃（原WAITING_RIDER），不再作为订单主状态使用，骑手抢单通过配送任务status=0体现
    DELIVERING(5, "配送中"),
    DELIVERED(6, "已送达"),
    COMPLETED(7, "已完成"),
    CANCELED(8, "已取消"),
    REFUNDING(9, "退款中"),
    REFUNDED(10, "已退款"),
    RIDER_ARRIVED(11, "骑手已到店");

    private final Integer code;
    private final String desc;

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus of(Integer code) {
        if (code == null) return null;
        // 4号状态码兼容旧数据：视为PREPARING（商家出餐但还未有骑手接单，实际业务中不会出现此订单状态）
        if (code.equals(4)) return PREPARING;
        for (OrderStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        // 兼容旧枚举名code：1=PAID→PENDING_ACCEPT, 2=MERCHANT_ACCEPTED→PREPARING, 3=RIDER_TAKEN→RIDER_ACCEPTED
        if (code.equals(1)) return PENDING_ACCEPT;
        if (code.equals(2)) return PREPARING;
        if (code.equals(3)) return RIDER_ACCEPTED;
        throw new IllegalArgumentException("非法订单状态: " + code);
    }

    /**
     * 合法的状态转移
     */
    public Set<OrderStatus> nextAllowed() {
        return switch (this) {
            case PENDING_PAY -> EnumSet.of(PENDING_ACCEPT, CANCELED);
            case PENDING_ACCEPT -> EnumSet.of(PREPARING, CANCELED, REFUNDING);
            case PREPARING -> EnumSet.of(RIDER_ACCEPTED, DELIVERED, CANCELED, REFUNDING);
            case RIDER_ACCEPTED -> EnumSet.of(RIDER_ARRIVED, PREPARING, CANCELED);
            case RIDER_ARRIVED -> EnumSet.of(DELIVERING, CANCELED);
            case DELIVERING -> EnumSet.of(DELIVERED, CANCELED);
            case DELIVERED -> EnumSet.of(COMPLETED, REFUNDING);
            case COMPLETED -> EnumSet.of(REFUNDING);
            case REFUNDING -> EnumSet.of(REFUNDED, COMPLETED, DELIVERED);
            case CANCELED, REFUNDED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitTo(OrderStatus target) {
        return nextAllowed().contains(target);
    }

    /**
     * 是否为终态
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELED || this == REFUNDED;
    }

    /**
     * 用户是否可以取消（备餐中/骑手接单前都可取消）
     */
    public boolean canUserCancel() {
        return this == PENDING_PAY || this == PENDING_ACCEPT || this == PREPARING;
    }

    /**
     * 是否需要骑手（配送中流程）
     */
    public boolean needRider() {
        return this == RIDER_ACCEPTED || this == RIDER_ARRIVED || this == DELIVERING;
    }

    /**
     * 配送任务是否应该存在（外卖单商家接单后就有配送任务）
     */
    public boolean hasDeliveryTask() {
        return this == PREPARING || this == RIDER_ACCEPTED || this == RIDER_ARRIVED
                || this == DELIVERING || this == DELIVERED;
    }
}
