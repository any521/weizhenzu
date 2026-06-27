package com.weizhenzu.infrastructure.thirdparty.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock 微信支付策略（毕业设计模拟实现）
 * - 非真实微信支付，模拟支付流程
 * - queryPayment 始终返回支付成功（前端有1.5秒loading+轮询间隔模拟等待）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Component
public class MockWechatStrategy implements PaymentStrategy {

    @Override
    public Integer payType() {
        return 2;
    }

    @Override
    public PaymentResult createPayment(PaymentRequest req) {
        log.info("[Mock微信支付] 模拟下单: paymentNo={}, amount={}", req.getPaymentNo(), req.getAmount());
        PaymentResult r = new PaymentResult();
        r.setSuccess(true);
        r.setPayUrl("about:blank");
        r.setMock(true);
        r.setThirdPartyNo("WX" + System.currentTimeMillis());
        return r;
    }

    @Override
    public PaymentResult queryPayment(String paymentNo) {
        log.info("[Mock微信支付] 模拟查询: {}", paymentNo);
        PaymentResult r = new PaymentResult();
        r.setSuccess(true);
        r.setPaid(true);
        r.setMock(true);
        r.setThirdPartyNo("MOCKWX" + paymentNo);
        return r;
    }

    @Override
    public RefundResult refund(RefundRequest req) {
        log.info("[Mock微信支付] 模拟退款: paymentNo={}, amount={}", req.getPaymentNo(), req.getAmount());
        RefundResult r = new RefundResult();
        r.setSuccess(true);
        r.setMock(true);
        r.setThirdPartyNo("MOCKWXRF" + System.currentTimeMillis());
        return r;
    }
}
