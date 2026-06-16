package com.weizhenzu.infrastructure.thirdparty.payment;

/**
 * 支付策略接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface PaymentStrategy {

    /**
     * 支付方式：1支付宝 2微信 3余额
     */
    Integer payType();

    /**
     * 创建支付
     */
    PaymentResult createPayment(PaymentRequest request);

    /**
     * 查询支付状态
     */
    PaymentResult queryPayment(String paymentNo);

    /**
     * 退款
     */
    RefundResult refund(RefundRequest request);
}
