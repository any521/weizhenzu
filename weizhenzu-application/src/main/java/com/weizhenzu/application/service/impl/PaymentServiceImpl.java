package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weizhenzu.application.service.PaymentService;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.common.utils.OrderNoGenerator;
import com.weizhenzu.domain.dto.PayDTO;
import com.weizhenzu.domain.dto.RefundApplyDTO;
import com.weizhenzu.domain.entity.Order;
import com.weizhenzu.domain.entity.Payment;
import com.weizhenzu.domain.entity.PaymentLog;
import com.weizhenzu.domain.entity.Refund;
import com.weizhenzu.domain.entity.RefundLog;
import com.weizhenzu.domain.enums.OrderStatus;
import com.weizhenzu.domain.enums.PaymentStatus;
import com.weizhenzu.domain.enums.RefundStatus;
import com.weizhenzu.domain.vo.PaymentVO;
import com.weizhenzu.domain.vo.RefundVO;
import com.weizhenzu.infrastructure.persistence.mapper.OrderMapper;
import com.weizhenzu.infrastructure.persistence.mapper.PaymentLogMapper;
import com.weizhenzu.infrastructure.persistence.mapper.PaymentMapper;
import com.weizhenzu.infrastructure.persistence.mapper.RefundLogMapper;
import com.weizhenzu.infrastructure.persistence.mapper.RefundMapper;
import com.weizhenzu.infrastructure.thirdparty.payment.PaymentRequest;
import com.weizhenzu.infrastructure.thirdparty.payment.PaymentResult;
import com.weizhenzu.infrastructure.thirdparty.payment.PaymentStrategy;
import com.weizhenzu.infrastructure.thirdparty.payment.PaymentStrategyFactory;
import com.weizhenzu.infrastructure.thirdparty.payment.RefundRequest;
import com.weizhenzu.infrastructure.thirdparty.payment.RefundResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

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
    private final RefundMapper refundMapper;
    private final RefundLogMapper refundLogMapper;
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
        Payment payment = getLatestPaymentByOrderId(orderId);
        return toVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO queryPaymentStatus(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        Payment payment = getLatestPaymentByOrderId(orderId);
        if (payment.getStatus() == PaymentStatus.SUCCESS.getCode()) {
            return toVO(payment);
        }
        // 调用第三方主动查询
        PaymentStrategy strategy = strategyFactory.get(payment.getPayType());
        PaymentResult result = strategy.queryPayment(payment.getPaymentNo());
        saveLog(payment.getId(), payment.getPaymentNo(), "QUERY",
                payment.getPaymentNo(), result.toString(), result.getSuccess() ? 1 : 0);

        if (Boolean.TRUE.equals(result.getPaid()) && payment.getStatus() != PaymentStatus.SUCCESS.getCode()) {
            handleCallback(payment.getPaymentNo(), result.getThirdPartyNo(), true);
            payment = paymentMapper.selectById(payment.getId());
        }
        return toVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String paymentNo, String thirdPartyNo, boolean success) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
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
            paymentMapper.updateStatus(payment.getId(),
                    PaymentStatus.SUCCESS.getCode(), thirdPartyNo);

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

    @Override
    public String handleAlipayNotify(Map<String, String> params) {
        String paymentNo = params.get("out_trade_no");
        String thirdPartyNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        log.info("[支付宝通知] paymentNo={}, tradeNo={}, status={}", paymentNo, thirdPartyNo, tradeStatus);

        // 1. 验签
        PaymentStrategy alipayStrategy = strategyFactory.get(1);
        if (!alipayStrategy.verifyNotify(params)) {
            log.warn("[支付宝通知] 验签失败: paymentNo={}", paymentNo);
            return "fail";
        }

        // 2. 处理状态
        boolean success = "TRADE_SUCCESS".equals(tradeStatus)
                || "TRADE_FINISHED".equals(tradeStatus);
        return handleCallback(paymentNo, thirdPartyNo, success);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO refund(Long orderId, RefundApplyDTO dto) {
        Long userId = UserContext.getUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getPayStatus() == null || order.getPayStatus() != 1) {
            throw new BizException(ResultCode.ORDER_REFUND_NOT_ALLOWED, "订单未支付，不可退款");
        }
        if (!OrderStatus.of(order.getStatus()).canTransitTo(OrderStatus.REFUNDING)) {
            throw new BizException(ResultCode.ORDER_REFUND_NOT_ALLOWED);
        }

        Payment payment = getLatestPaymentByOrderId(orderId);
        if (payment.getStatus() != PaymentStatus.SUCCESS.getCode()) {
            throw new BizException(ResultCode.REFUND_ERROR, "支付单状态不允许退款");
        }

        // 创建退款单
        Refund refund = new Refund();
        refund.setRefundNo(OrderNoGenerator.refundNo());
        refund.setOrderId(orderId);
        refund.setOrderNo(order.getOrderNo());
        refund.setPaymentId(payment.getId());
        refund.setUserId(userId);
        refund.setMerchantId(order.getMerchantId());
        refund.setAmount(dto.getAmount());
        refund.setReason(dto.getReason());
        refund.setStatus(RefundStatus.APPLYING.getCode());
        refundMapper.insert(refund);

        // 订单状态流转到退款中
        orderMapper.updateStatus(order.getId(), order.getStatus(), OrderStatus.REFUNDING.getCode());

        saveRefundLog(refund.getId(), refund.getRefundNo(), "APPLY",
                dto.toString(), "申请退款", 1);

        return toRefundVO(refund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePayment(Long paymentId) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BizException(ResultCode.NOT_FOUND, "支付单不存在");
        }
        if (payment.getStatus() != PaymentStatus.PENDING.getCode()) {
            throw new BizException(ResultCode.PAY_ERROR, "支付单状态不允许关闭");
        }
        paymentMapper.updateStatus(payment.getId(),
                PaymentStatus.CLOSED.getCode(), payment.getThirdPartyNo());
        saveLog(payment.getId(), payment.getPaymentNo(), "CLOSE",
                "手动关闭", "closed", 1);
    }

    private Payment getLatestPaymentByOrderId(Long orderId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .orderByDesc(Payment::getCreatedAt)
                        .last("LIMIT 1"));
        if (payment == null) {
            throw new BizException(ResultCode.NOT_FOUND, "支付单不存在");
        }
        return payment;
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

    private void saveRefundLog(Long refundId, String refundNo, String event,
                               String request, String response, Integer status) {
        RefundLog log = new RefundLog();
        log.setRefundId(refundId);
        log.setRefundNo(refundNo);
        log.setEvent(event);
        log.setRequest(request);
        log.setResponse(response);
        log.setStatus(status);
        refundLogMapper.insert(log);
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

    private RefundVO toRefundVO(Refund refund) {
        RefundVO vo = new RefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setOrderId(refund.getOrderId());
        vo.setOrderNo(refund.getOrderNo());
        vo.setAmount(refund.getAmount());
        vo.setReason(refund.getReason());
        vo.setStatus(refund.getStatus());
        vo.setStatusDesc(RefundStatus.of(refund.getStatus()).getDesc());
        vo.setAuditRemark(refund.getAuditRemark());
        vo.setRefundTime(refund.getRefundTime());
        vo.setCreatedAt(refund.getCreatedAt());
        return vo;
    }
}
