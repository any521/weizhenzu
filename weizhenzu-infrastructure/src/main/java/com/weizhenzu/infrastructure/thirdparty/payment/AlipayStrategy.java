package com.weizhenzu.infrastructure.thirdparty.payment;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付策略
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayStrategy implements PaymentStrategy {

    private final AlipayClient alipayClient;

    @Override
    public Integer payType() {
        return 1;
    }

    @Override
    public PaymentResult createPayment(PaymentRequest req) {
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setBizContent(String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\"," +
                "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}",
                req.getPaymentNo(), req.getAmount().toPlainString(), req.getSubject()));
            String form = alipayClient.pageExecute(request).getBody();
            PaymentResult r = new PaymentResult();
            r.setSuccess(true);
            r.setPayUrl(form);
            return r;
        } catch (Exception e) {
            log.error("支付宝下单失败: paymentNo={}", req.getPaymentNo(), e);
            PaymentResult r = new PaymentResult();
            r.setSuccess(false);
            r.setErrorMsg(e.getMessage());
            return r;
        }
    }

    @Override
    public PaymentResult queryPayment(String paymentNo) {
        // 简化实现：实际调用 AlipayTradeQueryRequest
        log.info("[支付宝] 查询支付状态: {}", paymentNo);
        PaymentResult r = new PaymentResult();
        r.setSuccess(true);
        return r;
    }

    @Override
    public RefundResult refund(RefundRequest req) {
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            request.setBizContent(String.format(
                "{\"out_trade_no\":\"%s\",\"refund_amount\":\"%s\",\"refund_reason\":\"%s\"," +
                "\"out_request_no\":\"%s\"}",
                req.getPaymentNo(), req.getAmount().toPlainString(),
                req.getReason(), req.getRefundNo()));
            var resp = alipayClient.execute(request);
            RefundResult r = new RefundResult();
            r.setSuccess("10000".equals(resp.getCode()));
            r.setErrorMsg(resp.getSubMsg());
            return r;
        } catch (Exception e) {
            log.error("支付宝退款失败: paymentNo={}", req.getPaymentNo(), e);
            RefundResult r = new RefundResult();
            r.setSuccess(false);
            r.setErrorMsg(e.getMessage());
            return r;
        }
    }
}
