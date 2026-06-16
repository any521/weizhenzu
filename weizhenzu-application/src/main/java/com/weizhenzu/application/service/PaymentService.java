package com.weizhenzu.application.service;

import com.weizhenzu.domain.dto.PayDTO;
import com.weizhenzu.domain.vo.PaymentVO;

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
     * 查询支付状态
     */
    PaymentVO queryPayment(Long orderId);

    /**
     * 支付回调（支付宝/微信）
     */
    String handleCallback(String paymentNo, String thirdPartyNo, boolean success);
}
