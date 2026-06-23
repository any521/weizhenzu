package com.weizhenzu.infrastructure.thirdparty.payment;

import java.util.Map;

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

    /**
     * 验证回调签名（仅支付宝需要实现）
     *
     * @param params 回调参数
     * @return true 验签通过
     */
    default boolean verifyNotify(Map<String, String> params) {
        return true;
    }

    /**
     * 查询退款状态
     *
     * @param refundNo 退款单号
     * @return 退款结果
     */
    default RefundResult queryRefund(String refundNo) {
        RefundResult r = new RefundResult();
        r.setSuccess(false);
        r.setErrorMsg("当前支付方式不支持退款查询");
        return r;
    }
}
