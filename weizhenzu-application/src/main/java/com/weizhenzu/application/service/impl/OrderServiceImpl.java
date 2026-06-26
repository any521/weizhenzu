package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weizhenzu.application.service.CouponService;
import com.weizhenzu.application.service.OrderService;
import com.weizhenzu.application.statemachine.OrderStateMachine;
import com.weizhenzu.common.annotation.DistributedLock;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.enums.UserTypeEnum;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.common.utils.OrderNoGenerator;
import com.weizhenzu.common.utils.PhoneUtils;
import com.weizhenzu.domain.dto.OrderCancelDTO;
import com.weizhenzu.domain.dto.OrderCreateDTO;
import com.weizhenzu.domain.dto.OrderNotifyMessage;
import com.weizhenzu.domain.entity.*;
import com.weizhenzu.domain.enums.DeliveryTaskStatus;
import com.weizhenzu.domain.enums.OrderStatus;
import com.weizhenzu.domain.enums.UserCouponStatus;
import com.weizhenzu.domain.vo.DeliveryTrackingVO;
import com.weizhenzu.domain.vo.OrderCreateVO;
import com.weizhenzu.domain.vo.OrderVO;
import com.weizhenzu.infrastructure.mq.OrderNotifyProducer;
import com.weizhenzu.infrastructure.persistence.mapper.*;
import com.weizhenzu.infrastructure.thirdparty.amap.AmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;
    private final DishMapper dishMapper;
    private final DishSpecMapper dishSpecMapper;
    private final AddressMapper addressMapper;
    private final MerchantMapper merchantMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final DeliveryManMapper deliveryManMapper;
    private final UserMapper userMapper;
    private final OrderStateMachine stateMachine;
    private final CouponService couponService;
    private final UserCouponMapper userCouponMapper;
    private final OrderNotifyProducer orderNotifyProducer;
    private final AmapService amapService;

    // 注意：@Idempotent 注解放在 Controller 层，Service 层不再重复标注，避免同一请求被两次拦截
    @DistributedLock(key = "'order:create:' + #dto.merchantId + ':' + T(com.weizhenzu.common.context.UserContext).getUserId()")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderCreateVO createOrder(OrderCreateDTO dto) {
        Long userId = UserContext.getUserId();

        // 0. 校验用户是否已绑定手机号（下单前强制绑定）
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        String decryptedPhone = PhoneUtils.decrypt(currentUser.getPhone());
        if (decryptedPhone == null || decryptedPhone.isBlank()) {
            throw new BizException(ResultCode.PHONE_NOT_BOUND, "请先绑定手机号再下单");
        }

        // 1. 校验地址
        Address addr = addressMapper.selectById(dto.getAddressId());
        if (addr == null || !userId.equals(addr.getUserId())) {
            throw new BizException(ResultCode.NOT_FOUND, "地址不存在");
        }

        // 2. 校验商家
        Merchant m = merchantMapper.selectById(dto.getMerchantId());
        if (m == null || m.getStatus() != 1) {
            throw new BizException(ResultCode.MERCHANT_NOT_FOUND);
        }
        if (m.getIsOpen() != 1) {
            throw new BizException(ResultCode.MERCHANT_NOT_OPEN);
        }

        // 3. 校验菜品 + 扣库存 + 计算总额
        BigDecimal total = BigDecimal.ZERO;
        int itemCount = 0;
        List<OrderItem> items = new ArrayList<>();
        for (OrderCreateDTO.OrderItemDTO i : dto.getItems()) {
            Dish dish = dishMapper.selectById(i.getDishId());
            if (dish == null || dish.getStatus() != 1) {
                throw new BizException(ResultCode.DISH_NOT_FOUND);
            }
            if (!dto.getMerchantId().equals(dish.getMerchantId())) {
                throw new BizException(ResultCode.PARAM_ERROR, "菜品不属于该商家");
            }

            BigDecimal unitPrice = dish.getPrice();
            String specName = null;
            if (i.getSpecId() != null) {
                DishSpec spec = dishSpecMapper.selectById(i.getSpecId());
                if (spec == null || spec.getStatus() != 1) {
                    throw new BizException(ResultCode.PARAM_ERROR, "规格不存在或已下架");
                }
                unitPrice = dish.getPrice().add(spec.getPriceDiff());
                specName = spec.getName();
                int rows = dishSpecMapper.deductStock(i.getSpecId(), i.getQuantity());
                if (rows == 0) {
                    throw new BizException(ResultCode.STOCK_NOT_ENOUGH, spec.getName() + " 库存不足");
                }
            }

            // 乐观锁扣菜品库存
            int rows = dishMapper.deductStock(i.getDishId(), i.getQuantity());
            if (rows == 0) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH, dish.getName() + " 库存不足");
            }

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(i.getQuantity()));
            total = total.add(subtotal);
            itemCount += i.getQuantity();

            OrderItem item = new OrderItem();
            item.setDishId(i.getDishId());
            item.setDishName(dish.getName());
            item.setDishImage(dish.getImage());
            item.setSpecId(i.getSpecId());
            item.setSpecName(specName);
            item.setUnitPrice(unitPrice);
            item.setQuantity(i.getQuantity());
            item.setSubtotal(subtotal);
            items.add(item);
        }

        // 4. 计算金额
        BigDecimal packingFee = m.getPackingFee() == null ? BigDecimal.ZERO : m.getPackingFee();
        BigDecimal deliveryFee = m.getDeliveryFee() == null ? BigDecimal.ZERO : m.getDeliveryFee();
        if (total.compareTo(m.getMinOrderAmount()) < 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "未达到起送价");
        }

        // 5. 计算优惠券折扣
        BigDecimal couponAmount = BigDecimal.ZERO;
        Long userCouponId = dto.getUserCouponId();
        UserCoupon usedCoupon = null;
        if (userCouponId != null) {
            couponAmount = couponService.calculateDiscount(userCouponId, total);
            if (couponAmount.compareTo(BigDecimal.ZERO) > 0) {
                usedCoupon = userCouponMapper.selectById(userCouponId);
                // 标记优惠券为已使用（锁定）
                usedCoupon.setStatus(UserCouponStatus.USED.getCode());
                usedCoupon.setUsedTime(LocalDateTime.now());
                userCouponMapper.updateById(usedCoupon);
            }
        }

        BigDecimal payAmount = total.add(packingFee).add(deliveryFee).subtract(couponAmount);
        // 最低支付0.01元
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            payAmount = new BigDecimal("0.01");
        }
        payAmount = payAmount.setScale(2, RoundingMode.HALF_UP);

        // 6. 创建订单主表
        Order order = new Order();
        order.setOrderNo(OrderNoGenerator.orderNo());
        order.setUserId(userId);
        order.setMerchantId(dto.getMerchantId());
        order.setAddressId(dto.getAddressId());
        order.setAddressSnapshot(buildAddressSnapshot(addr));
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setPayStatus(0);
        order.setItemCount(itemCount);
        order.setTotalAmount(total);
        order.setPackingFee(packingFee);
        order.setDeliveryFee(deliveryFee);
        order.setMerchantDiscount(BigDecimal.ZERO);
        order.setPlatformDiscount(BigDecimal.ZERO);
        order.setCouponAmount(couponAmount);
        order.setUserCouponId(userCouponId);
        order.setPayAmount(payAmount);
        order.setRemark(dto.getRemark());
        order.setDiningType(dto.getDiningType() != null ? dto.getDiningType() : 2); // 默认外卖
        order.setIsRated(0);
        order.setSource(1);
        order.setExpectedTime(LocalDateTime.now().plusMinutes(45));
        orderMapper.insert(order);

        // 回填优惠券的订单ID
        if (usedCoupon != null) {
            usedCoupon.setOrderId(order.getId());
            userCouponMapper.updateById(usedCoupon);
        }

        // 7. 创建订单明细
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            orderItemMapper.insert(item);
        }

        // 8. 记录订单日志
        saveOrderLog(order.getId(), order.getOrderNo(), null, OrderStatus.PENDING_PAY.getCode(),
                1, userId, "用户创建订单");

        // 9. 返回
        OrderCreateVO vo = new OrderCreateVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.PENDING_PAY.getDesc());
        vo.setPayAmount(payAmount);
        vo.setExpireTime(LocalDateTime.now().plusMinutes(30));
        return vo;
    }

    @Override
    public OrderVO detail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        // 用户归属校验：根据当前登录用户类型校验订单归属
        Long currentUserId = UserContext.getUserId();
        UserTypeEnum userType = UserContext.getUserType();
        if (userType != null) {
            boolean hasPermission = false;
            switch (userType) {
                case USER:
                    hasPermission = currentUserId.equals(order.getUserId());
                    break;
                case MERCHANT:
                    hasPermission = currentUserId.equals(order.getMerchantId());
                    break;
                case RIDER:
                    hasPermission = currentUserId.equals(order.getDeliveryManId());
                    break;
                case ADMIN:
                    hasPermission = true;
                    break;
                default:
                    break;
            }
            if (!hasPermission) {
                throw new BizException(ResultCode.FORBIDDEN);
            }
        }
        return toVO(order, true);
    }

    @Override
    public PageResult<OrderVO> userPage(Integer current, Integer size, Integer status) {
        Long userId = UserContext.getUserId();
        Page<Order> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        List<OrderVO> records = result.getRecords().stream()
                .map(o -> toVO(o, true))
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<OrderVO> merchantPage(Integer current, Integer size, Integer status) {
        Long merchantId = UserContext.getUserId();
        Page<Order> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        List<OrderVO> records = result.getRecords().stream()
                .map(o -> toVO(o, false))
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<OrderVO> riderPage(Integer current, Integer size, Integer status) {
        Long riderId = UserContext.getUserId();
        Page<Order> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getDeliveryManId, riderId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        List<OrderVO> records = result.getRecords().stream()
                .map(o -> toVO(o, false))
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, OrderCancelDTO dto) {
        Long userId = UserContext.getUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        OrderStatus from = OrderStatus.of(order.getStatus());
        stateMachine.transit(from, OrderStatus.CANCELED);

        int rows = orderMapper.updateStatus(orderId, order.getStatus(), OrderStatus.CANCELED.getCode());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更");
        }

        // 更新取消时间和取消原因
        orderMapper.updateCancelInfo(orderId, dto.getReason());

        // 回退库存
        rollbackStock(orderId);

        // 回退优惠券（如果使用了且订单未支付）
        rollbackCoupon(order);

        // 记录日志
        saveOrderLog(orderId, order.getOrderNo(), order.getStatus(),
                OrderStatus.CANCELED.getCode(), 1, userId, dto.getReason());

        // 如果已有骑手接单，通知骑手订单已取消
        if (order.getDeliveryManId() != null) {
            sendRiderNotifyAfterCommit(order, order.getStatus(), OrderStatus.CANCELED.getCode(),
                    1, userId, "ORDER_CANCELED", "订单已被用户取消");
        }
    }

    /**
     * 回退优惠券
     */
    private void rollbackCoupon(Order order) {
        if (order.getUserCouponId() != null && order.getPayStatus() != null && order.getPayStatus() == 0) {
            UserCoupon coupon = userCouponMapper.selectById(order.getUserCouponId());
            if (coupon != null && UserCouponStatus.USED.getCode().equals(coupon.getStatus())) {
                coupon.setStatus(UserCouponStatus.UNUSED.getCode());
                coupon.setUsedTime(null);
                coupon.setOrderId(null);
                userCouponMapper.updateById(coupon);
                log.info("[订单取消] 回退优惠券: couponId={}, orderId={}", coupon.getId(), order.getId());
            }
        }
    }

    /**
     * 自动取消超时未支付的订单（定时任务调用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoCancelTimeoutOrders(int timeoutMinutes) {
        // 1. 查询即将被取消的超时订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PENDING_PAY.getCode())
                .lt(Order::getCreatedAt, LocalDateTime.now().minusMinutes(timeoutMinutes));
        List<Order> timeoutOrders = orderMapper.selectList(wrapper);

        if (timeoutOrders.isEmpty()) {
            return 0;
        }

        // 2. 批量更新状态为已取消
        int canceledCount = orderMapper.cancelTimeoutOrders(timeoutMinutes);
        log.info("[自动取消] 超时订单批量取消: 待取消={}, 实际取消={}", timeoutOrders.size(), canceledCount);

        // 3. 逐个回退库存和优惠券
        for (Order order : timeoutOrders) {
            try {
                rollbackStock(order.getId());
                rollbackCoupon(order);
                saveOrderLog(order.getId(), order.getOrderNo(), OrderStatus.PENDING_PAY.getCode(),
                        OrderStatus.CANCELED.getCode(), 0, null, "超时未支付自动取消");
            } catch (Exception e) {
                log.error("[自动取消] 回退资源失败: orderId={}", order.getId(), e);
            }
        }

        return canceledCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceived(Long orderId) {
        Long userId = UserContext.getUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        OrderStatus from = OrderStatus.of(order.getStatus());
        stateMachine.transit(from, OrderStatus.COMPLETED);

        int rows = orderMapper.updateStatus(orderId, order.getStatus(), OrderStatus.COMPLETED.getCode());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更");
        }
        saveOrderLog(orderId, order.getOrderNo(), order.getStatus(),
                OrderStatus.COMPLETED.getCode(), 1, userId, "用户确认收货");

        // 更新商家月销和菜品销量
        updateSalesAfterComplete(order);

        // 通知骑手订单已完成
        if (order.getDeliveryManId() != null) {
            sendRiderNotifyAfterCommit(order, order.getStatus(), OrderStatus.COMPLETED.getCode(),
                    1, userId, "ORDER_COMPLETED", "用户已确认收货，配送完成");
        }
    }

    /**
     * 订单完成后更新销量：商家月销+1，菜品月销/总销累加数量
     */
    private void updateSalesAfterComplete(Order order) {
        try {
            // 更新商家月销（+1订单数）
            Merchant merchant = merchantMapper.selectById(order.getMerchantId());
            if (merchant != null) {
                int currentSales = merchant.getMonthSales() != null ? merchant.getMonthSales() : 0;
                merchant.setMonthSales(currentSales + 1);
                merchantMapper.updateById(merchant);
            }
            // 更新菜品月销和总销
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            for (OrderItem item : items) {
                if (item.getDishId() == null) continue;
                Dish dish = dishMapper.selectById(item.getDishId());
                if (dish != null) {
                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                    dish.setMonthSales((dish.getMonthSales() != null ? dish.getMonthSales() : 0) + qty);
                    dish.setTotalSales((dish.getTotalSales() != null ? dish.getTotalSales() : 0) + qty);
                    dishMapper.updateById(dish);
                }
            }
        } catch (Exception e) {
            log.error("更新销量失败: orderId={}", order.getId(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantAccept(Long orderId) {
        Long merchantId = UserContext.getUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !merchantId.equals(order.getMerchantId())) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        OrderStatus from = OrderStatus.of(order.getStatus());
        stateMachine.transit(from, OrderStatus.MERCHANT_ACCEPTED);

        int rows = orderMapper.updateStatus(orderId, order.getStatus(), OrderStatus.MERCHANT_ACCEPTED.getCode());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更");
        }
        int fromStatus = order.getStatus();
        saveOrderLog(orderId, order.getOrderNo(), fromStatus,
                OrderStatus.MERCHANT_ACCEPTED.getCode(), 2, merchantId, "商家接单");

        // 判断用餐类型：1=堂食，2=外卖（默认外卖）
        Integer diningType = order.getDiningType() != null ? order.getDiningType() : 2;
        boolean isTakeout = (diningType == 2);

        if (isTakeout) {
            // 外卖：创建配送任务，广播给骑手
            createDeliveryTask(order);

            // 事务提交后通知用户：商家已接单
            sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.MERCHANT_ACCEPTED.getCode(),
                    2, merchantId, "ORDER_ACCEPTED", "商家已接单，请耐心等待");

            // 事务提交后广播新订单给所有在线骑手
            sendNewOrderBroadcastAfterCommit(order);
        } else {
            // 堂食：不创建配送任务，直接进入制作流程
            // 事务提交后通知用户：商家已接单，正在备餐
            sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.MERCHANT_ACCEPTED.getCode(),
                    2, merchantId, "ORDER_ACCEPTED", "商家已接单，正在为您备餐");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantReject(Long orderId, String reason) {
        Long merchantId = UserContext.getUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !merchantId.equals(order.getMerchantId())) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        OrderStatus from = OrderStatus.of(order.getStatus());
        stateMachine.transit(from, OrderStatus.CANCELED);

        int rows = orderMapper.updateStatus(orderId, order.getStatus(), OrderStatus.CANCELED.getCode());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更");
        }
        int fromStatus = order.getStatus();
        rollbackStock(orderId);
        saveOrderLog(orderId, order.getOrderNo(), fromStatus,
                OrderStatus.CANCELED.getCode(), 2, merchantId, "商家拒单：" + reason);

        // 事务提交后通知用户：商家拒单
        String content = reason != null && !reason.isEmpty()
                ? "商家已拒单：" + reason : "商家已拒单，订单已取消";
        sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.CANCELED.getCode(),
                2, merchantId, "ORDER_REJECTED", content);

        // 如果已有骑手接单（极端情况），通知骑手订单已取消
        if (order.getDeliveryManId() != null) {
            sendRiderNotifyAfterCommit(order, fromStatus, OrderStatus.CANCELED.getCode(),
                    2, merchantId, "ORDER_CANCELED", "商家已拒单，订单已取消");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantReady(Long orderId) {
        Long merchantId = UserContext.getUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !merchantId.equals(order.getMerchantId())) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }

        Integer diningType = order.getDiningType() != null ? order.getDiningType() : 2;
        boolean isTakeout = (diningType == 2);

        if (isTakeout) {
            // 外卖：商家出餐完成 -> 等待骑手取餐，状态保持 MERCHANT_ACCEPTED
            saveOrderLog(orderId, order.getOrderNo(), order.getStatus(),
                    order.getStatus(), 2, merchantId, "商家出餐完成");

            // 事务提交后通知用户：商家出餐完成（状态不变但发送通知）
            sendOrderStatusNotifyAfterCommit(order, order.getStatus(), order.getStatus(),
                    2, merchantId, "ORDER_READY", "商家已出餐，骑手即将取餐");

            // 如果已有骑手接单，通知骑手出餐完成
            if (order.getDeliveryManId() != null) {
                sendRiderNotifyAfterCommit(order, order.getStatus(), order.getStatus(),
                        2, merchantId, "ORDER_READY", "商家已出餐，请尽快前往取餐");
            }
        } else {
            // 堂食：商家出餐完成 -> 状态变为DELIVERED（已出餐，等待用户取餐确认）
            OrderStatus from = OrderStatus.of(order.getStatus());
            stateMachine.transit(from, OrderStatus.DELIVERED);
            int rows = orderMapper.updateStatus(orderId, order.getStatus(), OrderStatus.DELIVERED.getCode());
            if (rows == 0) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更");
            }
            int fromStatus = order.getStatus();
            saveOrderLog(orderId, order.getOrderNo(), fromStatus,
                    OrderStatus.DELIVERED.getCode(), 2, merchantId, "商家已出餐，请取餐");

            // 事务提交后通知用户：堂食订单已出餐
            sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.DELIVERED.getCode(),
                    2, merchantId, "ORDER_READY", "您的餐品已做好，请取餐");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void riderGrab(Long taskId) {
        Long riderId = UserContext.getUserId();
        // 校验骑手是否已绑定手机号（接单前强制绑定）
        DeliveryMan rider = deliveryManMapper.selectById(riderId);
        if (rider == null) {
            throw new BizException(ResultCode.NOT_FOUND, "骑手不存在");
        }
        String riderPhone = PhoneUtils.decrypt(rider.getPhone());
        if (riderPhone == null || riderPhone.isBlank()) {
            throw new BizException(ResultCode.PHONE_NOT_BOUND, "请先绑定手机号再接单");
        }
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ResultCode.NOT_FOUND, "配送任务不存在");
        }
        int rows = deliveryTaskMapper.grab(taskId, riderId);
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "任务已被抢或已取消");
        }
        // 刷新task对象，确保grabTime等字段与DB一致
        task = deliveryTaskMapper.selectById(taskId);
        if (task.getGrabTime() == null) {
            task.setGrabTime(LocalDateTime.now());
            task.setStatus(DeliveryTaskStatus.GRABBED.getCode());
            task.setDeliveryManId(riderId);
            deliveryTaskMapper.updateById(task);
        }
        // 更新订单状态
        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            int fromStatus = order.getStatus();
            orderMapper.updateStatus(order.getId(), fromStatus,
                    OrderStatus.RIDER_TAKEN.getCode());
            order.setDeliveryManId(riderId);
            order.setDeliveryTaskId(taskId);
            orderMapper.updateById(order);
            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus,
                    OrderStatus.RIDER_TAKEN.getCode(), 3, riderId, "骑手接单");

            // 通知用户：骑手已接单
            sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.RIDER_TAKEN.getCode(),
                    3, riderId, "ORDER_RIDER_TAKEN", "骑手已接单，正在赶往商家");

            // 通知商家：骑手已接单
            sendMerchantNotifyAfterCommit(order, fromStatus, OrderStatus.RIDER_TAKEN.getCode(),
                    3, riderId, "ORDER_RIDER_TAKEN", "骑手已接单，正在赶来");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void riderArrive(Long taskId) {
        Long riderId = UserContext.getUserId();
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null || !riderId.equals(task.getDeliveryManId())) {
            throw new BizException(ResultCode.NOT_FOUND, "配送任务不存在");
        }
        deliveryTaskMapper.updateStatus(taskId, DeliveryTaskStatus.ARRIVED_MERCHANT.getCode());
        task.setArriveTime(LocalDateTime.now());
        task.setStatus(DeliveryTaskStatus.ARRIVED_MERCHANT.getCode());
        deliveryTaskMapper.updateById(task);

        // 到店后不改变订单状态，保持 RIDER_TAKEN(3)
        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            // 通知用户：骑手已到达商家
            sendOrderStatusNotifyAfterCommit(order, order.getStatus(), order.getStatus(),
                    3, riderId, "ORDER_STATUS_CHANGE", "骑手已到达商家，正在取餐");
            // 通知商家：骑手已到店
            sendMerchantNotifyAfterCommit(order, order.getStatus(), order.getStatus(),
                    3, riderId, "ORDER_STATUS_CHANGE", "骑手已到店");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void riderPickup(Long taskId) {
        Long riderId = UserContext.getUserId();
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null || !riderId.equals(task.getDeliveryManId())) {
            throw new BizException(ResultCode.NOT_FOUND, "配送任务不存在");
        }
        // 取餐后直接将task状态设置为4(DELIVERING配送中)
        deliveryTaskMapper.updateStatus(taskId, DeliveryTaskStatus.DELIVERING.getCode());
        // 补全arriveTime和pickupTime
        if (task.getArriveTime() == null) {
            task.setArriveTime(LocalDateTime.now());
        }
        task.setPickupTime(LocalDateTime.now());
        task.setStatus(DeliveryTaskStatus.DELIVERING.getCode());
        deliveryTaskMapper.updateById(task);

        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            int fromStatus = order.getStatus();
            // 订单状态直接设置为DELIVERING(5)
            orderMapper.updateStatus(order.getId(), fromStatus,
                    OrderStatus.DELIVERING.getCode());
            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus,
                    OrderStatus.DELIVERING.getCode(), 3, riderId, "骑手取餐，开始配送");

            // 通知用户：骑手已取餐
            sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.DELIVERING.getCode(),
                    3, riderId, "ORDER_PICKED_UP", "骑手已取餐，正在为您配送");

            // 通知商家：骑手已取餐
            sendMerchantNotifyAfterCommit(order, fromStatus, OrderStatus.DELIVERING.getCode(),
                    3, riderId, "ORDER_PICKED_UP", "骑手已取餐，正在配送");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void riderDeliver(Long taskId) {
        Long riderId = UserContext.getUserId();
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null || !riderId.equals(task.getDeliveryManId())) {
            throw new BizException(ResultCode.NOT_FOUND, "配送任务不存在");
        }
        deliveryTaskMapper.updateStatus(taskId, DeliveryTaskStatus.DELIVERED.getCode());
        task.setDeliverTime(LocalDateTime.now());
        task.setStatus(DeliveryTaskStatus.DELIVERED.getCode());
        deliveryTaskMapper.updateById(task);
        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            int fromStatus = order.getStatus();
            orderMapper.updateStatus(order.getId(), fromStatus,
                    OrderStatus.DELIVERED.getCode());
            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus,
                    OrderStatus.DELIVERED.getCode(), 3, riderId, "骑手送达");

            // 通知用户：订单已送达
            sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.DELIVERED.getCode(),
                    3, riderId, "ORDER_DELIVERED", "订单已送达，请及时取餐");

            // 通知商家：订单已送达
            sendMerchantNotifyAfterCommit(order, fromStatus, OrderStatus.DELIVERED.getCode(),
                    3, riderId, "ORDER_DELIVERED", "订单已送达");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void riderSendMessage(Long taskId, String content) {
        Long riderId = UserContext.getUserId();
        if (content == null || content.trim().isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "留言内容不能为空");
        }
        if (content.length() > 200) {
            content = content.substring(0, 200);
        }
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null || !riderId.equals(task.getDeliveryManId())) {
            throw new BizException(ResultCode.NOT_FOUND, "配送任务不存在");
        }
        Order order = orderMapper.selectById(task.getOrderId());
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }

        // 记录留言到订单日志（operatorType=3表示骑手）
        saveOrderLog(order.getId(), order.getOrderNo(), order.getStatus(),
                order.getStatus(), 3, riderId, "骑手留言：" + content);

        // 事务提交后推送给用户和商家
        sendRiderMessageAfterCommit(order, content);
    }

    @Override
    public DeliveryTrackingVO tracking(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        DeliveryTrackingVO vo = new DeliveryTrackingVO();
        vo.setOrderId(orderId);
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.of(order.getStatus()).getDesc());
        vo.setDiningType(order.getDiningType() != null ? order.getDiningType() : 2);

        // 构建步骤
        List<DeliveryTrackingVO.Step> steps = new ArrayList<>();
        boolean isTakeout = (vo.getDiningType() == 2);
        steps.add(buildStep("已下单", order.getCreatedAt(), true));
        steps.add(buildStep("商家接单", order.getMerchantAcceptTime(),
                order.getMerchantAcceptTime() != null));
        if (isTakeout) {
            // 外卖：骑手取餐→配送中→已送达
            steps.add(buildStep("骑手取餐", order.getRiderTakeTime(),
                    order.getRiderTakeTime() != null));
            steps.add(buildStep("配送中", order.getDeliverTime(),
                    order.getDeliverTime() != null));
        } else {
            // 堂食：制作中→已出餐
            steps.add(buildStep("制作中", order.getMerchantAcceptTime(),
                    order.getMerchantAcceptTime() != null));
            steps.add(buildStep("已出餐", order.getDeliverTime(),
                    order.getDeliverTime() != null));
        }
        steps.add(buildStep("已完成", order.getCompleteTime(),
                order.getCompleteTime() != null));
        vo.setSteps(steps);

        // 查询配送任务（提前查询，复用）
        DeliveryTask deliveryTask = null;
        if (order.getDeliveryTaskId() != null) {
            deliveryTask = deliveryTaskMapper.selectById(order.getDeliveryTaskId());
        }
        if (deliveryTask == null) {
            deliveryTask = deliveryTaskMapper.selectOne(
                    new LambdaQueryWrapper<DeliveryTask>()
                            .eq(DeliveryTask::getOrderId, orderId)
                            .last("LIMIT 1"));
        }

        // 骑手信息
        if (order.getDeliveryManId() != null) {
            DeliveryMan rider = deliveryManMapper.selectById(order.getDeliveryManId());
            if (rider != null) {
                DeliveryTrackingVO.RiderInfo ri = new DeliveryTrackingVO.RiderInfo();
                ri.setId(rider.getId());
                ri.setName(rider.getName());
                ri.setAvatar(rider.getAvatar());
                ri.setPhone(PhoneUtils.mask(rider.getPhone()));
                ri.setRating(rider.getRating());

                // 设置骑手位置
                BigDecimal riderLng = rider.getLongitude();
                BigDecimal riderLat = rider.getLatitude();
                vo.setRiderLng(riderLng);
                vo.setRiderLat(riderLat);
                ri.setLng(riderLng);
                ri.setLat(riderLat);

                // 根据配送任务状态分别计算到商家和到用户的距离
                Integer distToMerchant = null;
                Integer distToUser = null;
                String navTarget = "merchant";

                if (riderLng != null && riderLat != null && deliveryTask != null) {
                    // 骑手到商家距离
                    if (deliveryTask.getMerchantLng() != null && deliveryTask.getMerchantLat() != null) {
                        long dm = amapService.calculateDistance(
                                riderLng.doubleValue(), riderLat.doubleValue(),
                                deliveryTask.getMerchantLng().doubleValue(), deliveryTask.getMerchantLat().doubleValue());
                        if (dm >= 0) {
                            distToMerchant = (int) dm;
                        }
                    }
                    // 骑手到用户距离
                    if (deliveryTask.getUserLng() != null && deliveryTask.getUserLat() != null) {
                        long du = amapService.calculateDistance(
                                riderLng.doubleValue(), riderLat.doubleValue(),
                                deliveryTask.getUserLng().doubleValue(), deliveryTask.getUserLat().doubleValue());
                        if (du >= 0) {
                            distToUser = (int) du;
                        }
                    }
                    // 导航目标判断：配送任务状态 < 3（PICKED_UP之前）→ 目标是商家；>= 3 → 目标是用户
                    Integer taskStatus = deliveryTask.getStatus();
                    if (taskStatus != null && taskStatus >= com.weizhenzu.domain.enums.DeliveryTaskStatus.PICKED_UP.getCode()) {
                        navTarget = "user";
                    }
                }

                vo.setDistanceToMerchant(distToMerchant);
                vo.setDistanceToUser(distToUser);
                vo.setNavigationTarget(navTarget);
                // 当前显示的距离 = 导航目标对应的距离
                Integer currentDistance = "user".equals(navTarget) ? distToUser : distToMerchant;
                if (currentDistance != null) {
                    vo.setDistance(currentDistance);
                    ri.setDistance(currentDistance);
                }

                vo.setRider(ri);
            }
        }

        // 商家信息
        Merchant m = merchantMapper.selectById(order.getMerchantId());
        if (m != null) {
            DeliveryTrackingVO.MerchantInfo mi = new DeliveryTrackingVO.MerchantInfo();
            mi.setName(m.getName());
            mi.setAddress(m.getAddress());
            mi.setPhone(PhoneUtils.mask(m.getContactPhone()));
            mi.setLng(m.getLongitude());
            mi.setLat(m.getLatitude());
            vo.setMerchant(mi);
            // 设置商家经纬度到VO顶层
            vo.setMerchantLng(m.getLongitude());
            vo.setMerchantLat(m.getLatitude());
        }

        // 用户信息（从配送任务获取）
        if (deliveryTask != null) {
            // 设置用户经纬度到VO顶层
            vo.setUserLng(deliveryTask.getUserLng());
            vo.setUserLat(deliveryTask.getUserLat());

            DeliveryTrackingVO.UserInfo ui = new DeliveryTrackingVO.UserInfo();
            ui.setAddress(deliveryTask.getUserAddress());
            ui.setLng(deliveryTask.getUserLng());
            ui.setLat(deliveryTask.getUserLat());
            ui.setPhone(PhoneUtils.mask(deliveryTask.getUserPhone()));
            vo.setUserInfo(ui);
        }

        // 查询最近5条骑手留言（operatorType=3，remark以"骑手留言："开头）
        List<OrderLog> riderLogs = orderLogMapper.selectList(
                new LambdaQueryWrapper<OrderLog>()
                        .eq(OrderLog::getOrderId, orderId)
                        .eq(OrderLog::getOperatorType, 3)
                        .likeRight(OrderLog::getRemark, "骑手留言：")
                        .orderByDesc(OrderLog::getCreatedAt)
                        .last("LIMIT 5"));
        if (riderLogs != null && !riderLogs.isEmpty()) {
            List<DeliveryTrackingVO.RiderMessage> messages = new ArrayList<>();
            // 按时间正序排列
            for (int i = riderLogs.size() - 1; i >= 0; i--) {
                OrderLog logEntry = riderLogs.get(i);
                DeliveryTrackingVO.RiderMessage rm = new DeliveryTrackingVO.RiderMessage();
                rm.setId(logEntry.getId());
                String remark = logEntry.getRemark();
                if (remark != null && remark.startsWith("骑手留言：")) {
                    rm.setContent(remark.substring("骑手留言：".length()));
                } else {
                    rm.setContent(remark);
                }
                rm.setTime(logEntry.getCreatedAt());
                messages.add(rm);
            }
            vo.setRecentMessages(messages);
        }

        return vo;
    }

    private DeliveryTrackingVO.Step buildStep(String name, LocalDateTime time, boolean done) {
        DeliveryTrackingVO.Step s = new DeliveryTrackingVO.Step();
        s.setName(name);
        s.setTime(time);
        s.setDone(done);
        return s;
    }

    private void createDeliveryTask(Order order) {
        Merchant m = merchantMapper.selectById(order.getMerchantId());
        // 查询用户收货地址
        Address userAddr = null;
        if (order.getAddressId() != null) {
            userAddr = addressMapper.selectById(order.getAddressId());
        }
        DeliveryTask task = new DeliveryTask();
        task.setTaskNo(OrderNoGenerator.taskNo());
        task.setOrderId(order.getId());
        task.setOrderNo(order.getOrderNo());
        task.setMerchantId(order.getMerchantId());
        task.setMerchantName(m == null ? null : m.getName());
        task.setMerchantAddress(m == null ? null : m.getAddress());
        task.setMerchantLng(m == null ? null : m.getLongitude());
        task.setMerchantLat(m == null ? null : m.getLatitude());
        // 设置用户地址信息
        if (userAddr != null) {
            String fullAddress = (userAddr.getProvince() == null ? "" : userAddr.getProvince())
                    + (userAddr.getCity() == null ? "" : userAddr.getCity())
                    + (userAddr.getDistrict() == null ? "" : userAddr.getDistrict())
                    + (userAddr.getDetail() == null ? "" : userAddr.getDetail());
            task.setUserAddress(fullAddress);
            task.setUserLng(userAddr.getLongitude());
            task.setUserLat(userAddr.getLatitude());
            task.setUserPhone(userAddr.getContactPhone());
        }
        task.setStatus(0);
        task.setFee(order.getDeliveryFee());
        deliveryTaskMapper.insert(task);

        // 回填订单的deliveryTaskId
        order.setDeliveryTaskId(task.getId());
        orderMapper.updateById(order);
    }

    private void rollbackStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            dishMapper.rollbackStock(item.getDishId(), item.getQuantity());
        }
    }

    private void saveOrderLog(Long orderId, String orderNo, Integer fromStatus,
                              Integer toStatus, Integer operatorType, Long operatorId, String remark) {
        OrderLog log = new OrderLog();
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        orderLogMapper.insert(log);
    }

    /**
     * 事务提交后发送订单状态变更通知给用户
     * msgId使用确定性格式：orderId:type:user，确保幂等去重
     */
    private void sendOrderStatusNotifyAfterCommit(Order order, int fromStatus, int toStatus,
                                                   int operatorType, Long operatorId,
                                                   String msgType, String content) {
        final Order orderRef = order;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String msgId = orderRef.getId() + ":" + msgType + ":user";
                    OrderNotifyMessage msg = OrderNotifyMessage.builder()
                            .msgId(msgId)
                            .orderId(orderRef.getId())
                            .orderNo(orderRef.getOrderNo())
                            .merchantId(orderRef.getMerchantId())
                            .userId(orderRef.getUserId())
                            .amount(orderRef.getPayAmount())
                            .payType(orderRef.getPayType())
                            .fromStatus(fromStatus)
                            .toStatus(toStatus)
                            .operatorType(operatorType)
                            .operatorId(operatorId)
                            .eventTime(LocalDateTime.now())
                            .content(content)
                            .build();
                    msg.setType(msgType);
                    orderNotifyProducer.sendOrderStatusToUser(msg);
                } catch (Exception e) {
                    log.error("[订单通知] 发送用户通知失败: orderNo={}, type={}",
                            orderRef.getOrderNo(), msgType, e);
                }
            }
        });
    }

    /**
     * 事务提交后发送通知给骑手
     * msgId使用确定性格式：orderId:type:rider，确保幂等去重
     */
    private void sendRiderNotifyAfterCommit(Order order, int fromStatus, int toStatus,
                                             int operatorType, Long operatorId,
                                             String msgType, String content) {
        if (order.getDeliveryManId() == null) {
            return;
        }
        final Order orderRef = order;
        final Long riderId = order.getDeliveryManId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String msgId = orderRef.getId() + ":" + msgType + ":rider";
                    OrderNotifyMessage msg = OrderNotifyMessage.builder()
                            .msgId(msgId)
                            .orderId(orderRef.getId())
                            .orderNo(orderRef.getOrderNo())
                            .merchantId(orderRef.getMerchantId())
                            .userId(orderRef.getUserId())
                            .amount(orderRef.getPayAmount())
                            .payType(orderRef.getPayType())
                            .fromStatus(fromStatus)
                            .toStatus(toStatus)
                            .operatorType(operatorType)
                            .operatorId(operatorId)
                            .toUserId(riderId)
                            .eventTime(LocalDateTime.now())
                            .content(content)
                            .build();
                    msg.setType(msgType);
                    orderNotifyProducer.sendToRider(msg);
                } catch (Exception e) {
                    log.error("[订单通知] 发送骑手通知失败: orderNo={}, type={}",
                            orderRef.getOrderNo(), msgType, e);
                }
            }
        });
    }

    /**
     * 事务提交后发送通知给商家（骑手操作相关通知）
     * msgId使用确定性格式：orderId:type:merchant，确保幂等去重
     */
    private void sendMerchantNotifyAfterCommit(Order order, int fromStatus, int toStatus,
                                                int operatorType, Long operatorId,
                                                String msgType, String content) {
        final Order orderRef = order;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String msgId = orderRef.getId() + ":" + msgType + ":merchant";
                    OrderNotifyMessage msg = OrderNotifyMessage.builder()
                            .msgId(msgId)
                            .orderId(orderRef.getId())
                            .orderNo(orderRef.getOrderNo())
                            .merchantId(orderRef.getMerchantId())
                            .userId(orderRef.getUserId())
                            .amount(orderRef.getPayAmount())
                            .payType(orderRef.getPayType())
                            .fromStatus(fromStatus)
                            .toStatus(toStatus)
                            .operatorType(operatorType)
                            .operatorId(operatorId)
                            .eventTime(LocalDateTime.now())
                            .content(content)
                            .build();
                    msg.setType(msgType);
                    orderNotifyProducer.sendOrderStatusToMerchant(msg);
                } catch (Exception e) {
                    log.error("[订单通知] 发送商家通知失败: orderNo={}, type={}",
                            orderRef.getOrderNo(), msgType, e);
                }
            }
        });
    }

    /**
     * 事务提交后广播新订单给所有在线骑手
     */
    private void sendNewOrderBroadcastAfterCommit(Order order) {
        final Order orderRef = order;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    // 重新查询订单和商家信息（事务提交后）
                    Order freshOrder = orderMapper.selectById(orderRef.getId());
                    if (freshOrder == null) return;
                    Merchant merchant = merchantMapper.selectById(freshOrder.getMerchantId());

                    String msgId = freshOrder.getId() + ":NEW_ORDER:broadcast";
                    OrderNotifyMessage msg = OrderNotifyMessage.builder()
                            .msgId(msgId)
                            .orderId(freshOrder.getId())
                            .orderNo(freshOrder.getOrderNo())
                            .merchantId(freshOrder.getMerchantId())
                            .merchantName(merchant != null ? merchant.getName() : null)
                            .merchantAddress(merchant != null ? merchant.getAddress() : null)
                            .userId(freshOrder.getUserId())
                            .amount(freshOrder.getPayAmount())
                            .fee(freshOrder.getDeliveryFee())
                            .eventTime(LocalDateTime.now())
                            .content("有新的配送订单，快来抢单吧")
                            .build();
                    msg.setType("NEW_ORDER");
                    orderNotifyProducer.broadcastNewOrder(msg);
                } catch (Exception e) {
                    log.error("[订单通知] 广播新订单失败: orderNo={}", orderRef.getOrderNo(), e);
                }
            }
        });
    }

    /**
     * 事务提交后发送骑手留言通知给用户和商家
     * 留言类消息可多次发送，msgId加入nanoTime保证唯一性；MQ重投时msgId不变可被幂等去重
     */
    private void sendRiderMessageAfterCommit(Order order, String content) {
        final Order orderRef = order;
        final String msgContent = content;
        final long uniqueSuffix = System.nanoTime();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    // 通知用户
                    String userMsgId = orderRef.getId() + ":RIDER_MESSAGE:user:" + uniqueSuffix;
                    OrderNotifyMessage userMsg = OrderNotifyMessage.builder()
                            .msgId(userMsgId)
                            .orderId(orderRef.getId())
                            .orderNo(orderRef.getOrderNo())
                            .merchantId(orderRef.getMerchantId())
                            .userId(orderRef.getUserId())
                            .fromStatus(orderRef.getStatus())
                            .toStatus(orderRef.getStatus())
                            .operatorType(3)
                            .eventTime(LocalDateTime.now())
                            .content(msgContent)
                            .build();
                    userMsg.setType("RIDER_MESSAGE");
                    orderNotifyProducer.sendOrderStatusToUser(userMsg);

                    // 通知商家
                    String merchantMsgId = orderRef.getId() + ":RIDER_MESSAGE:merchant:" + uniqueSuffix;
                    OrderNotifyMessage merchantMsg = OrderNotifyMessage.builder()
                            .msgId(merchantMsgId)
                            .orderId(orderRef.getId())
                            .orderNo(orderRef.getOrderNo())
                            .merchantId(orderRef.getMerchantId())
                            .userId(orderRef.getUserId())
                            .fromStatus(orderRef.getStatus())
                            .toStatus(orderRef.getStatus())
                            .operatorType(3)
                            .eventTime(LocalDateTime.now())
                            .content(msgContent)
                            .build();
                    merchantMsg.setType("RIDER_MESSAGE");
                    orderNotifyProducer.sendOrderStatusToMerchant(merchantMsg);
                } catch (Exception e) {
                    log.error("[订单通知] 发送骑手留言通知失败: orderNo={}", orderRef.getOrderNo(), e);
                }
            }
        });
    }

    private String buildAddressSnapshot(Address addr) {
        return String.format(
                "{\"contactName\":\"%s\",\"contactPhone\":\"%s\",\"province\":\"%s\"," +
                "\"city\":\"%s\",\"district\":\"%s\",\"detail\":\"%s\"}",
                addr.getContactName(), addr.getContactPhone(), addr.getProvince(),
                addr.getCity(), addr.getDistrict(), addr.getDetail());
    }

    private OrderVO toVO(Order order, boolean withItems) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setMerchantId(order.getMerchantId());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.of(order.getStatus()).getDesc());
        vo.setPayStatus(order.getPayStatus());
        vo.setItemCount(order.getItemCount());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPackingFee(order.getPackingFee());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setMerchantDiscount(order.getMerchantDiscount());
        vo.setPlatformDiscount(order.getPlatformDiscount());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setRemark(order.getRemark());
        vo.setPayType(order.getPayType());
        vo.setPayTime(order.getPayTime());
        vo.setMerchantAcceptTime(order.getMerchantAcceptTime());
        vo.setRiderTakeTime(order.getRiderTakeTime());
        vo.setDeliverTime(order.getDeliverTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setExpectedTime(order.getExpectedTime());
        vo.setDeliveryTaskId(order.getDeliveryTaskId());
        vo.setIsRated(order.getIsRated());
        vo.setDiningType(order.getDiningType() != null ? order.getDiningType() : 2);
        vo.setCreatedAt(order.getCreatedAt());

        Merchant m = merchantMapper.selectById(order.getMerchantId());
        if (m != null) {
            vo.setMerchantName(m.getName());
            vo.setMerchantLogo(m.getLogo());
            vo.setMerchantPhone(m.getContactPhone());
        }

        // 填充配送任务相关字段
        DeliveryTask deliveryTask = null;
        if (order.getDeliveryTaskId() != null) {
            deliveryTask = deliveryTaskMapper.selectById(order.getDeliveryTaskId());
        }
        if (deliveryTask == null && order.getStatus() != null && order.getStatus() >= 3) {
            deliveryTask = deliveryTaskMapper.selectOne(
                    new LambdaQueryWrapper<DeliveryTask>()
                            .eq(DeliveryTask::getOrderId, order.getId())
                            .last("LIMIT 1"));
        }
        if (deliveryTask != null) {
            vo.setDeliveryTaskStatus(deliveryTask.getStatus());
            vo.setMerchantAddress(deliveryTask.getMerchantAddress());
            vo.setMerchantLng(deliveryTask.getMerchantLng());
            vo.setMerchantLat(deliveryTask.getMerchantLat());
            vo.setUserAddress(deliveryTask.getUserAddress());
            vo.setUserLng(deliveryTask.getUserLng());
            vo.setUserLat(deliveryTask.getUserLat());
            // 如果merchant中没有电话，从任务中兜底（任务本身不存商家电话，保持merchant来源）
        }

        if (withItems) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            vo.setItems(items.stream().map(i -> {
                OrderVO.OrderItemVO iv = new OrderVO.OrderItemVO();
                iv.setId(i.getId());
                iv.setDishId(i.getDishId());
                iv.setDishName(i.getDishName());
                iv.setDishImage(i.getDishImage());
                iv.setSpecId(i.getSpecId());
                iv.setSpecName(i.getSpecName());
                iv.setUnitPrice(i.getUnitPrice());
                iv.setQuantity(i.getQuantity());
                iv.setSubtotal(i.getSubtotal());
                return iv;
            }).collect(Collectors.toList()));
        }
        return vo;
    }
}
