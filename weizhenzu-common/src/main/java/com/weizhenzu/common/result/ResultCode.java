package com.weizhenzu.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统状态码枚举
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "成功"),
    FAIL(500, "系统繁忙"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // 用户业务 1xxxx
    USER_NOT_FOUND(10001, "用户不存在"),
    USER_DISABLED(10002, "用户已被禁用"),
    PHONE_FORMAT_ERROR(10003, "手机号格式错误"),
    SMS_CODE_ERROR(10004, "验证码错误或已过期"),
    SMS_CODE_FREQUENT(10005, "验证码发送过于频繁"),
    EMAIL_FORMAT_ERROR(10006, "邮箱格式错误"),
    EMAIL_NOT_FOUND(10007, "该邮箱尚未注册"),
    EMAIL_EXISTS(10008, "该邮箱已被注册"),
    PHONE_NOT_BOUND(10009, "尚未绑定手机号"),
    PHONE_ALREADY_BOUND(10010, "该手机号已被其他账号绑定"),
    EMAIL_SEND_FAIL(10011, "邮件发送失败，请稍后重试"),
    ACCOUNT_AUDITING(10012, "账号审核中，请耐心等待"),

    // 商家业务 2xxxx
    MERCHANT_NOT_FOUND(20001, "商家不存在"),
    MERCHANT_NOT_OPEN(20002, "商家未营业"),
    MERCHANT_AUDITING(20003, "商家审核中"),

    // 菜品业务 3xxxx
    DISH_NOT_FOUND(30001, "菜品不存在"),
    DISH_OFFLINE(30002, "菜品已下架"),
    STOCK_NOT_ENOUGH(30003, "库存不足"),

    // 订单业务 4xxxx
    ORDER_NOT_FOUND(40001, "订单不存在"),
    ORDER_STATUS_ERROR(40002, "订单状态不合法"),
    ORDER_AMOUNT_ERROR(40003, "订单金额异常"),
    ORDER_REPEAT_SUBMIT(40004, "请勿重复提交订单"),
    ORDER_CANCEL_NOT_ALLOWED(40005, "当前状态不可取消"),
    ORDER_REFUND_NOT_ALLOWED(40006, "当前状态不可退款"),

    // 支付业务 5xxxx
    PAY_ERROR(50001, "支付失败"),
    PAY_REPEAT(50002, "订单已支付"),
    PAY_CALLBACK_ERROR(50003, "支付回调异常"),
    REFUND_ERROR(50004, "退款失败"),

    // 优惠券业务 6xxxx
    COUPON_NOT_FOUND(60001, "优惠券不存在"),
    COUPON_RECEIVED_FULL(60002, "优惠券已被领完"),
    COUPON_RECEIVED_LIMIT(60003, "已达领取上限"),
    COUPON_USED(60004, "优惠券已使用"),
    COUPON_EXPIRED(60005, "优惠券已过期"),
    COUPON_NOT_MATCH(60006, "优惠券不满足使用条件"),

    // 配送业务 7xxxx
    NO_AVAILABLE_RIDER(70001, "暂无可用骑手"),

    // 限流/幂等 9xxxx
    RATE_LIMIT(90001, "请求过于频繁，请稍后再试"),
    IDEMPOTENT(90002, "请勿重复操作");

    private final Integer code;
    private final String message;
}
