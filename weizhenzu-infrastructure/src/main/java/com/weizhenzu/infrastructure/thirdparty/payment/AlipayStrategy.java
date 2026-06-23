package com.weizhenzu.infrastructure.thirdparty.payment;

import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝支付策略（手机网站支付 WAP）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayStrategy implements PaymentStrategy {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;

    @Override
    public Integer payType() {
        return 1;
    }

    @Override
    public PaymentResult createPayment(PaymentRequest req) {
        try {
            AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
            request.setNotifyUrl(alipayConfig.getNotifyUrl());
            request.setReturnUrl(alipayConfig.getReturnUrl());

            // 使用 bizModel 替代字符串拼接，避免注入风险
            com.alipay.api.domain.AlipayTradeWapPayModel model =
                    new com.alipay.api.domain.AlipayTradeWapPayModel();
            model.setOutTradeNo(req.getPaymentNo());
            model.setTotalAmount(req.getAmount().toPlainString());
            model.setSubject(req.getSubject());
            model.setProductCode("QUICK_WAP_WAY");
            if (alipayConfig.getSellerId() != null && !alipayConfig.getSellerId().isEmpty()) {
                model.setSellerId(alipayConfig.getSellerId());
            }
            request.setBizModel(model);

            // WAP 支付返回完整的跳转 URL（GET 方式）
            String payUrl = alipayClient.pageExecute(request, "GET").getBody();
            PaymentResult r = new PaymentResult();
            r.setSuccess(true);
            r.setPayUrl(payUrl);
            return r;
        } catch (Exception e) {
            log.error("[支付宝] 下单失败: paymentNo={}", req.getPaymentNo(), e);
            PaymentResult r = new PaymentResult();
            r.setSuccess(false);
            r.setErrorMsg(e.getMessage());
            return r;
        }
    }

    @Override
    public PaymentResult queryPayment(String paymentNo) {
        PaymentResult r = new PaymentResult();
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            com.alipay.api.domain.AlipayTradeQueryModel model =
                    new com.alipay.api.domain.AlipayTradeQueryModel();
            model.setOutTradeNo(paymentNo);
            request.setBizModel(model);

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            r.setSuccess("10000".equals(response.getCode()));
            r.setThirdPartyNo(response.getTradeNo());

            // 解析支付状态
            String tradeStatus = response.getTradeStatus();
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                r.setPaid(true);
            } else {
                r.setPaid(false);
            }
            if (!r.getSuccess()) {
                r.setErrorMsg(response.getSubMsg());
            }
            log.info("[支付宝] 查询支付状态: paymentNo={}, tradeStatus={}", paymentNo, tradeStatus);
            return r;
        } catch (Exception e) {
            log.error("[支付宝] 查询支付状态失败: paymentNo={}", paymentNo, e);
            r.setSuccess(false);
            r.setPaid(false);
            r.setErrorMsg(e.getMessage());
            return r;
        }
    }

    @Override
    public RefundResult refund(RefundRequest req) {
        RefundResult r = new RefundResult();
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            com.alipay.api.domain.AlipayTradeRefundModel model =
                    new com.alipay.api.domain.AlipayTradeRefundModel();
            model.setOutTradeNo(req.getPaymentNo());
            model.setRefundAmount(req.getAmount().toPlainString());
            model.setRefundReason(req.getReason());
            model.setOutRequestNo(req.getRefundNo());
            request.setBizModel(model);

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            r.setSuccess("10000".equals(response.getCode()));
            r.setThirdPartyNo(response.getTradeNo());
            if (!r.getSuccess()) {
                r.setErrorMsg(response.getSubMsg());
            }
            log.info("[支付宝] 退款: paymentNo={}, refundNo={}, success={}",
                    req.getPaymentNo(), req.getRefundNo(), r.getSuccess());
            return r;
        } catch (Exception e) {
            log.error("[支付宝] 退款失败: paymentNo={}", req.getPaymentNo(), e);
            r.setSuccess(false);
            r.setErrorMsg(e.getMessage());
            return r;
        }
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params, alipayConfig.getPublicKey(), alipayConfig.getCharset(), alipayConfig.getSignType());
            log.info("[支付宝] 回调验签结果: {}", signVerified);
            return signVerified;
        } catch (Exception e) {
            log.error("[支付宝] 回调验签异常", e);
            return false;
        }
    }

    @Override
    public RefundResult queryRefund(String refundNo) {
        RefundResult r = new RefundResult();
        try {
            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel model =
                    new com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel();
            model.setOutRequestNo(refundNo);
            request.setBizModel(model);

            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
            r.setSuccess("10000".equals(response.getCode()));
            r.setThirdPartyNo(response.getTradeNo());
            if (!r.getSuccess()) {
                r.setErrorMsg(response.getSubMsg());
            }
            return r;
        } catch (Exception e) {
            log.error("[支付宝] 查询退款状态失败: refundNo={}", refundNo, e);
            r.setSuccess(false);
            r.setErrorMsg(e.getMessage());
            return r;
        }
    }

    /**
     * 金额转字符串（保留 2 位小数）
     */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
