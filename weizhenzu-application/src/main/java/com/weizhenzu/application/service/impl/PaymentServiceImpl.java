package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weizhenzu.application.service.PaymentService;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.common.utils.OrderNoGenerator;
import com.weizhenzu.domain.dto.OrderNotifyMessage;
import com.weizhenzu.domain.dto.PayDTO;
import com.weizhenzu.domain.dto.RefundApplyDTO;
import com.weizhenzu.domain.entity.Order;
import com.weizhenzu.domain.entity.Payment;
import com.weizhenzu.domain.entity.PaymentLog;
import com.weizhenzu.domain.entity.Refund;
import com.weizhenzu.domain.entity.RefundLog;
import com.weizhenzu.domain.entity.User;
import com.weizhenzu.domain.enums.OrderStatus;
import com.weizhenzu.domain.enums.PaymentStatus;
import com.weizhenzu.domain.enums.RefundStatus;
import com.weizhenzu.domain.vo.PaymentVO;
import com.weizhenzu.domain.vo.RefundVO;
import com.weizhenzu.infrastructure.mq.OrderNotifyProducer;
import com.weizhenzu.infrastructure.persistence.mapper.OrderMapper;
import com.weizhenzu.infrastructure.persistence.mapper.PaymentLogMapper;
import com.weizhenzu.infrastructure.persistence.mapper.PaymentMapper;
import com.weizhenzu.infrastructure.persistence.mapper.RefundLogMapper;
import com.weizhenzu.infrastructure.persistence.mapper.RefundMapper;
import com.weizhenzu.infrastructure.persistence.mapper.UserMapper;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final UserMapper userMapper;
    private final PaymentStrategyFactory strategyFactory;
    private final OrderNotifyProducer orderNotifyProducer;

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
        payment.setExpireTime(LocalDateTime.now().plusMinutes(30));
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

        // 余额支付：即时扣减余额并标记支付成功
        if (dto.getPayType() == 3 && Boolean.TRUE.equals(result.getPaid())) {
            processBalancePayment(payment, order, userId);
        }

        return toVO(payment);
    }

    /**
     * 处理余额支付：扣减用户余额，更新支付单和订单状态
     */
    private void processBalancePayment(Payment payment, Order order, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "用户不存在");
        }
        if (user.getBalance() == null || user.getBalance().compareTo(payment.getAmount()) < 0) {
            throw new BizException(ResultCode.PAY_ERROR, "余额不足");
        }
        // 扣减余额
        user.setBalance(user.getBalance().subtract(payment.getAmount()));
        userMapper.updateById(user);

        // 标记支付成功（乐观锁：仅当状态为PENDING时更新，防止并发重复处理）
        int payRows = paymentMapper.updateStatus(payment.getId(),
                PaymentStatus.SUCCESS.getCode(), "BALANCE_" + payment.getPaymentNo(),
                PaymentStatus.PENDING.getCode());

        // 更新订单状态
        boolean orderPaid = false;
        if (payRows > 0 && order.getStatus() == OrderStatus.PENDING_PAY.getCode()) {
            int orderRows = orderMapper.updateStatus(order.getId(),
                    OrderStatus.PENDING_PAY.getCode(), OrderStatus.PENDING_ACCEPT.getCode());
            if (orderRows > 0) {
                orderMapper.updatePayStatus(order.getId(), 1);
                orderPaid = true;
            }
        }

        saveLog(payment.getId(), payment.getPaymentNo(), "BALANCE_PAY",
                "余额支付扣减: " + payment.getAmount(), "success", 1);
        log.info("[余额支付] 支付成功: paymentNo={}, userId={}, amount={}",
                payment.getPaymentNo(), userId, payment.getAmount());

        // 事务提交后发送MQ通知商家
        if (orderPaid) {
            final Order orderRef = order;
            final Payment paymentRef = payment;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNewOrderNotify(orderRef, paymentRef);
                }
            });
        }
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
    public PaymentVO queryPaymentByNo(String paymentNo) {
        Payment payment = getPaymentByNo(paymentNo);
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
        return syncPaymentStatus(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO queryPaymentStatusByNo(String paymentNo) {
        Payment payment = getPaymentByNo(paymentNo);
        if (payment.getStatus() == PaymentStatus.SUCCESS.getCode()) {
            return toVO(payment);
        }
        return syncPaymentStatus(payment);
    }

    /**
     * 主动同步支付状态（调用第三方查询并更新本地状态）
     */
    private PaymentVO syncPaymentStatus(Payment payment) {
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
    public void closePaymentByNo(String paymentNo) {
        Payment payment = getPaymentByNo(paymentNo);
        closePayment(payment.getId());
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
            // 更新支付单状态为成功（乐观锁：仅当状态为PENDING时更新，防止并发回调重复处理）
            int payRows = paymentMapper.updateStatus(payment.getId(),
                    PaymentStatus.SUCCESS.getCode(), thirdPartyNo,
                    PaymentStatus.PENDING.getCode());

            // 更新订单状态
            Order order = orderMapper.selectById(payment.getOrderId());
            boolean orderPaid = false;
            if (payRows > 0 && order != null && order.getStatus() == OrderStatus.PENDING_PAY.getCode()) {
                // 使用乐观锁更新订单状态：待付款 -> 待接单
                int orderRows = orderMapper.updateStatus(order.getId(),
                        OrderStatus.PENDING_PAY.getCode(), OrderStatus.PENDING_ACCEPT.getCode());
                if (orderRows > 0) {
                    orderMapper.updatePayStatus(order.getId(), 1);
                    // 回写支付方式：确保 order.payType 与最终成功的 payment 一致，
                    // 避免用户中途切换支付方式后 order.payType 与实际支付方式不符
                    if (payment.getPayType() != null) {
                        orderMapper.updatePayType(order.getId(), payment.getPayType());
                    }
                    orderPaid = true;
                    log.info("[支付回调] 订单支付成功: orderId={}, paymentNo={}, payType={}",
                            order.getId(), paymentNo, payment.getPayType());
                }
            } else if (order != null) {
                // 订单状态不是待付款（可能已经被取消或其他），仅记录日志
                log.warn("[支付回调] 订单状态异常，不更新状态: orderId={}, status={}, paymentNo={}",
                        order.getId(), order.getStatus(), paymentNo);
                // 仍需确保支付状态为已支付
                if (order.getPayStatus() == null || order.getPayStatus() != 1) {
                    orderMapper.updatePayStatus(order.getId(), 1);
                }
                // 即使订单状态异常，也回写支付方式以保证准确性
                if (payment.getPayType() != null) {
                    orderMapper.updatePayType(order.getId(), payment.getPayType());
                }
            }

            // 事务提交后发送MQ通知商家（避免事务回滚导致消息误发）
            if (orderPaid && order != null) {
                final Order orderRef = order;
                final Payment paymentRef = payment;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sendNewOrderNotify(orderRef, paymentRef);
                    }
                });
            }

            saveLog(payment.getId(), paymentNo, "CALLBACK",
                    "thirdPartyNo=" + thirdPartyNo, "success", 1);
            return "success";
        } else {
            paymentMapper.updateStatusForce(payment.getId(),
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
                PaymentStatus.CLOSED.getCode(), payment.getThirdPartyNo(),
                PaymentStatus.PENDING.getCode());
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

    private Payment getPaymentByNo(String paymentNo) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPaymentNo, paymentNo)
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

    /**
     * 发送新订单通知到MQ（事务提交后调用）
     * msgId使用确定性格式：orderId:type，确保同一事件重复生产时可被幂等去重
     */
    private void sendNewOrderNotify(Order order, Payment payment) {
        try {
            String msgId = order.getId() + ":NEW_ORDER:merchant";
            OrderNotifyMessage msg = OrderNotifyMessage.builder()
                    .msgId(msgId)
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .merchantId(order.getMerchantId())
                    .userId(order.getUserId())
                    .amount(payment.getAmount())
                    .payType(payment.getPayType())
                    .fromStatus(OrderStatus.PENDING_PAY.getCode())
                    .toStatus(OrderStatus.PENDING_ACCEPT.getCode())
                    .operatorType(1)
                    .operatorId(order.getUserId())
                    .eventTime(LocalDateTime.now())
                    .content("您有新订单，请及时处理")
                    .build();
            orderNotifyProducer.sendNewOrderToMerchant(msg);
        } catch (Exception e) {
            log.error("[支付回调] 发送新订单通知失败: orderNo={}", order.getOrderNo(), e);
        }
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
