package com.weizhenzu.infrastructure.thirdparty.payment;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付策略工厂
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Component
public class PaymentStrategyFactory {

    private final Map<Integer, PaymentStrategy> map = new HashMap<>();

    public PaymentStrategyFactory(List<PaymentStrategy> strategies) {
        strategies.forEach(s -> map.put(s.payType(), s));
    }

    public PaymentStrategy get(Integer payType) {
        PaymentStrategy s = map.get(payType);
        if (s == null) {
            throw new IllegalArgumentException("不支持的支付方式: " + payType);
        }
        return s;
    }
}
