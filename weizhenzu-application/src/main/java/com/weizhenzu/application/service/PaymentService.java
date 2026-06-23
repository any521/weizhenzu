package com.weizhenzu.application.service;

import com.weizhenzu.domain.dto.PayDTO;
import com.weizhenzu.domain.dto.RefundApplyDTO;
import com.weizhenzu.domain.vo.PaymentVO;
import com.weizhenzu.domain.vo.RefundVO;

import java.util.Map;

/**
 * 支付服务接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface PaymentService {

    /**
     * 创建支付
     */
    PaymentVO createPayment(PayDTO dto);

    /**
     * 查询支付状态（DB）
     */
    PaymentVO queryPayment(Long orderId);

    /**
     * 主动查询支付状态（调用第三方同步状态）
     */
    PaymentVO queryPaymentStatus(Long orderId);

    /**
     * 支付回调（支付宝/微信）
     */
    String handleCallback(String paymentNo, String thirdPartyNo, boolean success);

    /**
     * 处理支付宝异步通知（含验签）
     */
    String handleAlipayNotify(Map<String, String> params);

    /**
     * 退款
     *
     * @param orderId 订单 ID
     * @param dto     退款申请
     * @return 退款信息
     */
    RefundVO refund(Long orderId, RefundApplyDTO dto);

    /**
     * 关闭支付单
     */
    void closePayment(Long paymentId);
}
