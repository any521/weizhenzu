package com.weizhenzu.application.service.impl;

import com.weizhenzu.application.service.PaymentService;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.common.utils.OrderNoGenerator;
import com.weizhenzu.domain.dto.PayDTO;
import com.weizhenzu.domain.entity.Order;
import com.weizhenzu.domain.entity.Payment;
import com.weizhenzu.domain.entity.PaymentLog;
import com.weizhenzu.domain.enums.OrderStatus;
import com.weizhenzu.domain.enums.PaymentStatus;
import com.weizhenzu.domain.vo.PaymentVO;
import com.weizhenzu.infrastructure.persistence.mapper.OrderMapper;
import com.weizhenzu.infrastructure.persistence.mapper.PaymentLogMapper;
import com.weizhenzu.infrastructure.persistence.mapper.PaymentMapper;
import com.weizhenzu.infrastructure.thirdparty.payment.PaymentRequest;
import com.weizhenzu.infrastructure.thirdparty.payment.PaymentResult;
import com.weizhenzu.infrastructure.thirdparty.payment.PaymentStrategy;
import com.weizhenzu.infrastructure.thirdparty.payment.PaymentStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付服务实现
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final OrderMapper orderMapper;
    private final PaymentStrategyFactory strategyFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO createPayment(PayDTO dto) {
        Long userId = UserContext.getUserId();
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getPayStatus() != null && order.getPayStatus() == 1) {
            throw new BizException(ResultCode.PAY_REPEAT);
        }

        // 创建支付单
        Payment payment = new Payment();
        payment.setPaymentNo(OrderNoGenerator.paymentNo());
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(userId);
        payment.setAmount(order.getPayAmount());
        payment.setPayType(dto.getPayType());
        payment.setStatus(PaymentStatus.PENDING.getCode());
        payment.setExpireTime(LocalDateTime.now().plusMinutes(15));
        paymentMapper.insert(payment);

        // 调用支付策略
        PaymentStrategy strategy = strategyFactory.get(dto.getPayType());
        PaymentRequest req = new PaymentRequest();
        req.setPaymentNo(payment.getPaymentNo());
        req.setOrderNo(order.getOrderNo());
        req.setAmount(order.getPayAmount());
        req.setSubject("味真足订单-" + order.getOrderNo());
        req.setUserId(String.valueOf(userId));
        PaymentResult result = strategy.createPayment(req);

        // 记录流水
        saveLog(payment.getId(), payment.getPaymentNo(), "CREATE",
                req.toString(), result.toString(), result.getSuccess() ? 1 : 0);

        if (!result.getSuccess()) {
            throw new BizException(ResultCode.PAY_ERROR, result.getErrorMsg());
        }

        // 更新支付URL
        payment.setPayUrl(result.getPayUrl());
        paymentMapper.updateById(payment);

        // 更新订单支付方式
        order.setPayType(dto.getPayType());
        orderMapper.updateById(order);

        return toVO(payment);
    }

    @Override
    public PaymentVO queryPayment(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        Payment payment = paymentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .orderByDesc(Payment::getCreatedAt)
                        .last("LIMIT 1"));
        if (payment == null) {
            throw new BizException(ResultCode.NOT_FOUND, "支付单不存在");
        }
        return toVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String paymentNo, String thirdPartyNo, boolean success) {
        Payment payment = paymentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPaymentNo, paymentNo)
                        .last("LIMIT 1"));
        if (payment == null) {
            log.warn("[支付回调] 支付单不存在: {}", paymentNo);
            return "fail";
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS.getCode()) {
            log.info("[支付回调] 支付单已处理: {}", paymentNo);
            return "success";
        }

        if (success) {
            // 更新支付单状态
            paymentMapper.updateStatus(payment.getId(),
                    PaymentStatus.SUCCESS.getCode(), thirdPartyNo);

            // 更新订单状态
            Order order = orderMapper.selectById(payment.getOrderId());
            if (order != null && order.getStatus() == OrderStatus.PENDING_PAY.getCode()) {
                orderMapper.updateStatus(order.getId(),
                        OrderStatus.PENDING_PAY.getCode(), OrderStatus.PAID.getCode());
                orderMapper.updatePayStatus(order.getId(), 1);
                order.setPayTime(LocalDateTime.now());
                orderMapper.updateById(order);
            }

            saveLog(payment.getId(), paymentNo, "CALLBACK",
                    "thirdPartyNo=" + thirdPartyNo, "success", 1);
            return "success";
        } else {
            paymentMapper.updateStatus(payment.getId(),
                    PaymentStatus.FAIL.getCode(), thirdPartyNo);
            saveLog(payment.getId(), paymentNo, "CALLBACK",
                    "thirdPartyNo=" + thirdPartyNo, "fail", 0);
            return "fail";
        }
    }

    private void saveLog(Long paymentId, String paymentNo, String event,
                         String request, String response, Integer status) {
        PaymentLog log = new PaymentLog();
        log.setPaymentId(paymentId);
        log.setPaymentNo(paymentNo);
        log.setEvent(event);
        log.setRequest(request);
        log.setResponse(response);
        log.setStatus(status);
        paymentLogMapper.insert(log);
    }

    private PaymentVO toVO(Payment payment) {
        PaymentVO vo = new PaymentVO();
        vo.setPaymentNo(payment.getPaymentNo());
        vo.setOrderId(payment.getOrderId());
        vo.setOrderNo(payment.getOrderNo());
        vo.setAmount(payment.getAmount());
        vo.setPayType(payment.getPayType());
        vo.setStatus(payment.getStatus());
        vo.setPayUrl(payment.getPayUrl());
        vo.setThirdPartyNo(payment.getThirdPartyNo());
        return vo;
    }
}
