package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weizhenzu.application.service.CartService;
import com.weizhenzu.application.service.CouponService;
import com.weizhenzu.application.service.OrderService;
import com.weizhenzu.application.service.PaymentService;
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
import com.weizhenzu.domain.dto.OrderPreviewDTO;
import com.weizhenzu.domain.dto.OrderNotifyMessage;
import com.weizhenzu.domain.dto.RefundApplyDTO;
import com.weizhenzu.domain.entity.*;
import com.weizhenzu.domain.enums.DeliveryTaskStatus;
import com.weizhenzu.domain.enums.OrderStatus;
import com.weizhenzu.domain.enums.PaymentStatus;
import com.weizhenzu.domain.enums.RefundStatus;
import com.weizhenzu.domain.enums.UserCouponStatus;
import com.weizhenzu.domain.vo.DeliveryTrackingVO;
import com.weizhenzu.domain.vo.OrderCreateVO;
import com.weizhenzu.domain.vo.OrderPreviewVO;
import com.weizhenzu.domain.vo.OrderVO;
import com.weizhenzu.infrastructure.mq.OrderNotifyProducer;
import com.weizhenzu.infrastructure.persistence.mapper.*;
import com.weizhenzu.infrastructure.thirdparty.payment.PaymentStrategy;
import com.weizhenzu.infrastructure.thirdparty.payment.PaymentStrategyFactory;
import com.weizhenzu.infrastructure.thirdparty.payment.RefundRequest;
import com.weizhenzu.infrastructure.thirdparty.payment.RefundResult;
import com.weizhenzu.infrastructure.thirdparty.amap.AmapService;
import com.weizhenzu.infrastructure.thirdparty.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    /** 地理围栏：商家半径（米），到店/取餐时需在此范围内 */
    private static final int GEOFENCE_MERCHANT_RADIUS = 200;
    /** 地理围栏：用户收货地址半径（米），送达时需在此范围内 */
    private static final int GEOFENCE_USER_RADIUS = 300;
    /** 课设展示模式开关：true 时跳过骑手到店/取餐/送达的地理围栏校验，直接放行。
     *  展示结束后改回 false 即可恢复严格校验。 */
    private static final boolean SKIP_GEOFENCE = true;

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
    private final CartService cartService;
    private final UserCouponMapper userCouponMapper;
    private final OrderNotifyProducer orderNotifyProducer;
    private final AmapService amapService;
    private final StorageService storageService;
    private final PaymentService paymentService;
    private final RefundMapper refundMapper;
    private final PaymentMapper paymentMapper;
    private final PaymentStrategyFactory paymentStrategyFactory;
    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private OrderService self; // 自注入，用于调用本类的REQUIRES_NEW事务方法

    @Override
    public OrderPreviewVO previewOrder(OrderPreviewDTO dto) {
        Long userId = UserContext.getUserId();

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

        // 3. 校验菜品 + 计算商品总额（预览不扣库存）
        BigDecimal total = BigDecimal.ZERO;
        for (OrderPreviewDTO.OrderItemDTO i : dto.getItems()) {
            Dish dish = dishMapper.selectById(i.getDishId());
            if (dish == null || dish.getStatus() != 1) {
                throw new BizException(ResultCode.DISH_NOT_FOUND);
            }
            if (!dto.getMerchantId().equals(dish.getMerchantId())) {
                throw new BizException(ResultCode.PARAM_ERROR, "菜品不属于该商家");
            }
            BigDecimal unitPrice = dish.getPrice();
            if (i.getSpecId() != null) {
                DishSpec spec = dishSpecMapper.selectById(i.getSpecId());
                if (spec == null || spec.getStatus() != 1) {
                    throw new BizException(ResultCode.PARAM_ERROR, "规格不存在或已下架");
                }
                unitPrice = dish.getPrice().add(spec.getPriceDiff());
            }
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(i.getQuantity())));
        }

        // 4. 计算打包费、配送费（与 createOrder 完全一致的口径）
        BigDecimal packingFee = m.getPackingFee() == null ? BigDecimal.ZERO : m.getPackingFee();
        Integer diningType = dto.getDiningType() != null ? dto.getDiningType() : 2;
        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (diningType == 2) {
            BigDecimal baseDeliveryFee = m.getDeliveryFee() == null ? BigDecimal.ZERO : m.getDeliveryFee();
            deliveryFee = calculateDeliveryFee(baseDeliveryFee, m, addr);
        }

        // 5. 优惠券抵扣（预览不锁定券，仅计算金额）
        BigDecimal couponAmount = BigDecimal.ZERO;
        if (dto.getUserCouponId() != null) {
            try {
                couponAmount = couponService.calculateDiscount(dto.getUserCouponId(), total, dto.getMerchantId());
            } catch (Exception e) {
                log.warn("[订单预览] 优惠券计算失败，忽略: userCouponId={}, err={}", dto.getUserCouponId(), e.getMessage());
                couponAmount = BigDecimal.ZERO;
            }
        }

        BigDecimal payAmount = total.add(packingFee).add(deliveryFee).subtract(couponAmount);
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            payAmount = new BigDecimal("0.01");
        }
        payAmount = payAmount.setScale(2, RoundingMode.HALF_UP);

        boolean reachMin = !(diningType == 2 && total.compareTo(m.getMinOrderAmount()) < 0);

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setMerchantId(dto.getMerchantId());
        vo.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        vo.setPackingFee(packingFee.setScale(2, RoundingMode.HALF_UP));
        vo.setDeliveryFee(deliveryFee.setScale(2, RoundingMode.HALF_UP));
        vo.setCouponAmount(couponAmount.setScale(2, RoundingMode.HALF_UP));
        vo.setPayAmount(payAmount);
        vo.setDiningType(diningType);
        vo.setReachMinAmount(reachMin);
        vo.setMinOrderAmount(m.getMinOrderAmount());
        return vo;
    }

    // 注意：@Idempotent 注解放在 Controller 层，Service 层不再重复标注，避免同一请求被两次拦截
    @DistributedLock(key = "'order:create:' + #dto.merchantId + ':' + T(com.weizhenzu.common.context.UserContext).getUserId()")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderCreateVO createOrder(OrderCreateDTO dto) {
        Long userId = UserContext.getUserId();
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

        // 确定用餐类型：1=堂食，2=外卖，3=自取，默认外卖
        Integer diningType = dto.getDiningType() != null ? dto.getDiningType() : 2;

        // 配送费计算：只有外卖(diningType=2)才计算配送费；堂食(1)和自取(3)配送费为0
        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (diningType == 2) {
            BigDecimal baseDeliveryFee = m.getDeliveryFee() == null ? BigDecimal.ZERO : m.getDeliveryFee();
            deliveryFee = calculateDeliveryFee(baseDeliveryFee, m, addr);
        }

        if (diningType == 2 && total.compareTo(m.getMinOrderAmount()) < 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "未达到起送价");
        }

        // 5. 计算优惠券折扣（含商家范围校验）
        BigDecimal couponAmount = BigDecimal.ZERO;
        Long userCouponId = dto.getUserCouponId();
        UserCoupon usedCoupon = null;
        if (userCouponId != null) {
            couponAmount = couponService.calculateDiscount(userCouponId, total, dto.getMerchantId());
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
        order.setDiningType(diningType);
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

        // 9. 清空该商家的购物车项
        try {
            cartService.clearByMerchant(userId, dto.getMerchantId());
        } catch (Exception e) {
            log.warn("[订单] 清空购物车失败(不影响订单创建): orderId={}, merchantId={}", order.getId(), dto.getMerchantId(), e);
        }

        // 10. 返回
        OrderCreateVO vo = new OrderCreateVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.PENDING_PAY.getDesc());
        vo.setTotalAmount(total);
        vo.setPackingFee(packingFee);
        vo.setDeliveryFee(deliveryFee);
        vo.setCouponAmount(couponAmount);
        vo.setPayAmount(payAmount);
        vo.setDiningType(diningType);
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
        if (!from.canUserCancel()) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不允许取消");
        }

        // 已支付订单：不能直接取消，需走退款流程（转为 REFUNDING），避免资金滞留
        if (order.getPayStatus() != null && order.getPayStatus() == 1) {
            RefundApplyDTO refundDto = new RefundApplyDTO();
            refundDto.setReason("用户取消订单：" + dto.getReason());
            refundDto.setAmount(order.getPayAmount());
            paymentService.refund(orderId, refundDto);

            // 回退库存（退款流程不会自动回退库存）
            rollbackStock(orderId);
            // 回退优惠券（已支付订单的优惠券在退款完成时由退款流程处理，此处仅回退未支付场景）
            rollbackCoupon(order);

            // 取消未抢/已抢/到店的配送任务
            cancelDeliveryTaskIfActive(order);

            // 记录日志
            saveOrderLog(orderId, order.getOrderNo(), order.getStatus(),
                    OrderStatus.REFUNDING.getCode(), 1, userId, "用户取消已支付订单，转入退款流程");

            // 通知骑手订单已取消（如有）
            if (order.getDeliveryManId() != null) {
                sendRiderNotifyAfterCommit(order, order.getStatus(), OrderStatus.REFUNDING.getCode(),
                        1, userId, "ORDER_CANCELED", "订单已被用户取消，请停止配送");
            }
            return;
        }

        // 未支付订单：直接取消
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

        // 取消配送任务
        cancelDeliveryTaskIfActive(order);

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
     * 取消订单关联的配送任务（如果任务处于待抢/已抢/到店状态）
     */
    private void cancelDeliveryTaskIfActive(Order order) {
        if (order.getDeliveryTaskId() == null) {
            return;
        }
        DeliveryTask task = deliveryTaskMapper.selectById(order.getDeliveryTaskId());
        if (task != null && task.getStatus() != null) {
            Integer taskStatus = task.getStatus();
            boolean canCancelTask = taskStatus.equals(DeliveryTaskStatus.PENDING_GRAB.getCode())
                    || taskStatus.equals(DeliveryTaskStatus.GRABBED.getCode())
                    || taskStatus.equals(DeliveryTaskStatus.ARRIVED_MERCHANT.getCode());
            if (canCancelTask && !DeliveryTaskStatus.CANCELED.getCode().equals(taskStatus)) {
                deliveryTaskMapper.updateStatus(task.getId(), DeliveryTaskStatus.CANCELED.getCode());
            }
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
     * 改为逐条独立事务处理：每条订单的"状态更新+库存回退+优惠券回退+日志"在独立事务中完成，
     * 单条失败不影响其他订单，避免批量UPDATE后回滚失败导致数据不一致。
     */
    @Override
    public int autoCancelTimeoutOrders(int timeoutMinutes) {
        // 1. 查询待取消的超时订单（仅查ID列表，避免长事务锁行）
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PENDING_PAY.getCode())
                .lt(Order::getCreatedAt, threshold)
                .select(Order::getId)
                .last("LIMIT 200");
        List<Order> timeoutOrders = orderMapper.selectList(wrapper);

        if (timeoutOrders.isEmpty()) {
            return 0;
        }

        log.info("[自动取消] 待处理超时订单数: {}", timeoutOrders.size());

        // 2. 逐条独立事务处理（通过self调用REQUIRES_NEW方法）
        int canceledCount = 0;
        for (Order order : timeoutOrders) {
            try {
                boolean ok = self.cancelSingleTimeoutOrder(order.getId());
                if (ok) canceledCount++;
            } catch (Exception e) {
                log.error("[自动取消] 单条订单处理失败: orderId={}", order.getId(), e);
            }
        }

        log.info("[自动取消] 超时订单处理完成: 成功={}, 总数={}", canceledCount, timeoutOrders.size());
        return canceledCount;
    }

    /**
     * 取消单条超时未支付订单（独立事务REQUIRES_NEW）
     * 乐观锁更新状态+回退库存+回退优惠券+记录日志，全部在一个事务内，要么全成功要么全回滚。
     * @return true=取消成功，false=订单状态已变更（被其他线程处理）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean cancelSingleTimeoutOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        // 乐观锁：只有PENDING_PAY状态才取消
        int rows = orderMapper.updateStatus(orderId, OrderStatus.PENDING_PAY.getCode(), OrderStatus.CANCELED.getCode());
        if (rows == 0) {
            log.info("[自动取消] 订单状态已变更，跳过: orderId={}", orderId);
            return false;
        }
        // 更新取消时间和原因
        orderMapper.updateCancelInfo(orderId, "超时未支付自动取消");
        // 回退库存和优惠券
        rollbackStock(orderId);
        rollbackCoupon(order);
        // 记录日志
        saveOrderLog(orderId, order.getOrderNo(), OrderStatus.PENDING_PAY.getCode(),
                OrderStatus.CANCELED.getCode(), 0, null, "超时未支付自动取消");
        log.info("[自动取消] 订单超时取消成功: orderId={}, orderNo={}", orderId, order.getOrderNo());
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean processMerchantTimeoutOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        // 乐观锁：只有PENDING_ACCEPT状态才处理
        int rows = orderMapper.updateStatus(orderId, OrderStatus.PENDING_ACCEPT.getCode(), OrderStatus.REFUNDING.getCode());
        if (rows == 0) {
            log.info("[商家超时] 订单状态已变更，跳过: orderId={}", orderId);
            return false;
        }

        // 回退库存和优惠券
        rollbackStock(orderId);
        rollbackCoupon(order);
        // 取消配送任务（若有）
        cancelDeliveryTaskIfActive(order);

        // 查找已支付的支付单
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Payment> payWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getStatus, PaymentStatus.SUCCESS.getCode())
                        .last("LIMIT 1");
        Payment payment = paymentMapper.selectOne(payWrapper);

        if (payment == null) {
            // 无支付记录：直接取消
            orderMapper.updateStatus(orderId, OrderStatus.REFUNDING.getCode(), OrderStatus.CANCELED.getCode());
            saveOrderLog(orderId, order.getOrderNo(), OrderStatus.PENDING_ACCEPT.getCode(),
                    OrderStatus.CANCELED.getCode(), 0, null, "商家超时未接单自动取消（无支付记录）");
            // 通知用户
            OrderNotifyMessage userMsg = OrderNotifyMessage.builder()
                    .msgId(orderId + ":MERCHANT_TIMEOUT_CANCEL:" + System.currentTimeMillis())
                    .orderId(orderId).orderNo(order.getOrderNo()).userId(order.getUserId())
                    .type("ORDER_MERCHANT_TIMEOUT")
                    .content("商家超时未接单，订单已自动取消")
                    .fromStatus(OrderStatus.PENDING_ACCEPT.getCode()).toStatus(OrderStatus.CANCELED.getCode())
                    .eventTime(LocalDateTime.now()).build();
            orderNotifyProducer.sendOrderStatusToUser(userMsg);
            return true;
        }

        // 创建退款单
        Refund refund = new Refund();
        refund.setRefundNo(OrderNoGenerator.refundNo());
        refund.setOrderId(orderId);
        refund.setOrderNo(order.getOrderNo());
        refund.setPaymentId(payment.getId());
        refund.setUserId(order.getUserId());
        refund.setMerchantId(order.getMerchantId());
        refund.setAmount(payment.getAmount());
        refund.setReason("商家超时未接单自动取消退款");
        refund.setStatus(RefundStatus.REFUNDING.getCode());
        refundMapper.insert(refund);

        // 调用第三方退款
        PaymentStrategy strategy = paymentStrategyFactory.get(payment.getPayType());
        boolean refundSuccess = false;
        String thirdPartyNo = null;
        String errorMsg = null;

        if (strategy != null) {
            try {
                RefundRequest req = new RefundRequest();
                req.setPaymentNo(payment.getPaymentNo());
                req.setRefundNo(refund.getRefundNo());
                req.setAmount(payment.getAmount());
                req.setReason("商家超时未接单自动退款");
                RefundResult result = strategy.refund(req);
                refundSuccess = Boolean.TRUE.equals(result.getSuccess());
                thirdPartyNo = result.getThirdPartyNo();
                errorMsg = result.getErrorMsg();
            } catch (Exception e) {
                log.error("[商家超时] 调用第三方退款异常: orderNo={}, paymentNo={}",
                        order.getOrderNo(), payment.getPaymentNo(), e);
                errorMsg = e.getMessage();
            }
        } else {
            // 余额支付等场景直接标记为退款成功
            refundSuccess = true;
        }

        if (refundSuccess) {
            refundMapper.updateStatus(refund.getId(), RefundStatus.REFUNDED.getCode(),
                    thirdPartyNo != null ? thirdPartyNo : "SYSTEM_AUTO", null);
            orderMapper.updateStatus(orderId, OrderStatus.REFUNDING.getCode(), OrderStatus.REFUNDED.getCode());
            paymentMapper.updateStatusForce(payment.getId(), PaymentStatus.REFUNDED.getCode(), thirdPartyNo);

            saveOrderLog(orderId, order.getOrderNo(), OrderStatus.PENDING_ACCEPT.getCode(),
                    OrderStatus.REFUNDED.getCode(), 0, null, "商家超时未接单自动取消并退款成功");

            // 通知用户
            OrderNotifyMessage userMsg = OrderNotifyMessage.builder()
                    .msgId(orderId + ":MERCHANT_TIMEOUT_REFUND:" + System.currentTimeMillis())
                    .orderId(orderId).orderNo(order.getOrderNo()).userId(order.getUserId())
                    .type("ORDER_MERCHANT_TIMEOUT").amount(payment.getAmount())
                    .content("商家超时未接单，订单已自动取消并退款，退款金额：" + payment.getAmount() + "元")
                    .fromStatus(OrderStatus.PENDING_ACCEPT.getCode()).toStatus(OrderStatus.REFUNDED.getCode())
                    .eventTime(LocalDateTime.now()).build();
            orderNotifyProducer.sendOrderStatusToUser(userMsg);

            // 通知商家
            OrderNotifyMessage merchantMsg = OrderNotifyMessage.builder()
                    .msgId(orderId + ":MERCHANT_TIMEOUT_CANCEL:" + System.currentTimeMillis())
                    .orderId(orderId).orderNo(order.getOrderNo()).merchantId(order.getMerchantId())
                    .type("ORDER_MERCHANT_TIMEOUT")
                    .content("您有订单因超时未接单被系统自动取消：" + order.getOrderNo())
                    .fromStatus(OrderStatus.PENDING_ACCEPT.getCode()).toStatus(OrderStatus.REFUNDED.getCode())
                    .eventTime(LocalDateTime.now()).build();
            orderNotifyProducer.sendOrderCancelToMerchant(merchantMsg);

            log.info("[商家超时] 订单退款成功: orderNo={}, refundNo={}", order.getOrderNo(), refund.getRefundNo());
            return true;
        } else {
            // 退款失败：保持REFUNDING，告警管理员
            refundMapper.updateStatus(refund.getId(), RefundStatus.REFUNDING.getCode(),
                    "自动退款失败：" + errorMsg, null);
            saveOrderLog(orderId, order.getOrderNo(), OrderStatus.PENDING_ACCEPT.getCode(),
                    OrderStatus.REFUNDING.getCode(), 0, null, "商家超时未接单自动取消，退款发起失败：" + errorMsg);

            OrderNotifyMessage alertMsg = OrderNotifyMessage.builder()
                    .msgId(orderId + ":MERCHANT_TIMEOUT_REFUND_FAIL:" + System.currentTimeMillis())
                    .orderId(orderId).orderNo(order.getOrderNo())
                    .type("ADMIN_ALERT").alertLevel("ERROR")
                    .content("订单" + order.getOrderNo() + "商家超时自动取消退款失败，请人工处理。错误：" + errorMsg)
                    .eventTime(LocalDateTime.now()).build();
            orderNotifyProducer.sendAdminAlert(alertMsg);

            log.warn("[商家超时] 订单退款失败: orderNo={}, error={}", order.getOrderNo(), errorMsg);
            return false;
        }
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
        // 校验订单状态必须是PENDING_ACCEPT(1)
        OrderStatus from = OrderStatus.of(order.getStatus());
        if (from != OrderStatus.PENDING_ACCEPT) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单当前状态不允许商家接单");
        }
        stateMachine.transit(from, OrderStatus.PREPARING);

        int rows = orderMapper.updateStatus(orderId, order.getStatus(), OrderStatus.PREPARING.getCode());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更");
        }
        int fromStatus = order.getStatus();

        // 同步Java对象状态（updateStatus已在DB中更新status和version，需同步到Java对象避免后续updateById覆盖）
        order.setStatus(OrderStatus.PREPARING.getCode());
        if (order.getVersion() != null) {
            order.setVersion(order.getVersion() + 1);
        }

        // 设置商家接单时间
        order.setMerchantAcceptTime(LocalDateTime.now());

        Integer diningType = order.getDiningType() != null ? order.getDiningType() : 2;
        boolean isTakeout = (diningType == 2);

        if (isTakeout) {
            // 外卖订单：立即创建配送任务（初始状态PENDING_GRAB(0)），备餐和骑手赶往商家并行
            // createDeliveryTask内部会设置order.deliveryTaskId并调用orderMapper.updateById(order)，
            // 此时merchantAcceptTime已在order对象上，会一并保存
            createDeliveryTask(order);
        } else {
            // 堂食/自取订单：不创建配送任务，保存merchantAcceptTime
            orderMapper.updateById(order);
        }

        saveOrderLog(orderId, order.getOrderNo(), fromStatus,
                OrderStatus.PREPARING.getCode(), 2, merchantId, "商家接单");

        // 通知用户
        String userNotifyContent = isTakeout
                ? "商家已接单，正在备餐，骑手即将赶来"
                : "商家已接单，正在备餐";
        sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.PREPARING.getCode(),
                2, merchantId, "ORDER_ACCEPTED", userNotifyContent);

        if (isTakeout) {
            // 外卖：事务提交后广播新订单给所有在线骑手抢单
            sendNewOrderBroadcastAfterCommit(order);
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
        int fromStatus = order.getStatus();

        // 已支付订单：不能直接拒单取消，需走退款流程，避免用户资金滞留
        if (order.getPayStatus() != null && order.getPayStatus() == 1) {
            RefundApplyDTO refundDto = new RefundApplyDTO();
            refundDto.setReason("商家拒单：" + (reason != null ? reason : ""));
            refundDto.setAmount(order.getPayAmount());
            paymentService.refund(orderId, refundDto);

            // 回退库存
            rollbackStock(orderId);
            rollbackCoupon(order);
            cancelDeliveryTaskIfActive(order);

            saveOrderLog(orderId, order.getOrderNo(), fromStatus,
                    OrderStatus.REFUNDING.getCode(), 2, merchantId, "商家拒单，转入退款：" + reason);

            // 通知用户：商家拒单，将退款
            String content = reason != null && !reason.isEmpty()
                    ? "商家已拒单：" + reason + "，退款将原路返回" : "商家已拒单，退款将原路返回";
            sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.REFUNDING.getCode(),
                    2, merchantId, "ORDER_REJECTED", content);

            if (order.getDeliveryManId() != null) {
                sendRiderNotifyAfterCommit(order, fromStatus, OrderStatus.REFUNDING.getCode(),
                        2, merchantId, "ORDER_CANCELED", "商家已拒单，订单已取消");
            }
            return;
        }

        // 未支付订单：直接取消
        stateMachine.transit(from, OrderStatus.CANCELED);

        int rows = orderMapper.updateStatus(orderId, order.getStatus(), OrderStatus.CANCELED.getCode());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更");
        }
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

        OrderStatus from = OrderStatus.of(order.getStatus());
        int fromStatus = order.getStatus();

        if (isTakeout) {
            // 外卖：校验状态为PREPARING(2)或RIDER_ACCEPTED(3)或RIDER_ARRIVED(11)
            if (from != OrderStatus.PREPARING
                    && from != OrderStatus.RIDER_ACCEPTED
                    && from != OrderStatus.RIDER_ARRIVED) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单当前状态不允许商家出餐");
            }
            // 外卖：不改变订单主状态，只设置merchantReadyTime
            order.setMerchantReadyTime(LocalDateTime.now());
            orderMapper.updateById(order);

            saveOrderLog(orderId, order.getOrderNo(), fromStatus,
                    fromStatus, 2, merchantId, "商家已出餐");

            // 配送任务在merchantAccept时已创建，此处不需要再创建

            // 如果已有骑手接单（RIDER_ACCEPTED或RIDER_ARRIVED），通知骑手"商家已出餐，请尽快取餐"
            if (from == OrderStatus.RIDER_ACCEPTED || from == OrderStatus.RIDER_ARRIVED) {
                sendRiderNotifyAfterCommit(order, fromStatus, fromStatus,
                        2, merchantId, "ORDER_READY", "商家已出餐，请尽快取餐");
            }
        } else {
            // 堂食/自取：PREPARING(2) → DELIVERED(6)（出餐即待取餐）
            stateMachine.transit(from, OrderStatus.DELIVERED);
            int rows = orderMapper.updateStatus(orderId, order.getStatus(), OrderStatus.DELIVERED.getCode());
            if (rows == 0) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更");
            }

            // 同步Java对象状态
            order.setStatus(OrderStatus.DELIVERED.getCode());
            if (order.getVersion() != null) {
                order.setVersion(order.getVersion() + 1);
            }

            // 设置出餐时间和送达时间（堂食/自取出餐即待取餐）
            order.setMerchantReadyTime(LocalDateTime.now());
            order.setDeliverTime(LocalDateTime.now());
            orderMapper.updateById(order);

            saveOrderLog(orderId, order.getOrderNo(), fromStatus,
                    OrderStatus.DELIVERED.getCode(), 2, merchantId, "商家已出餐，请取餐");

            // 通知用户：堂食订单已出餐
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
            // 校验订单当前状态必须是PREPARING(2)（商家接单后即创建配送任务广播抢单，此时订单处于备餐中）
            OrderStatus from = OrderStatus.of(fromStatus);
            if (from != OrderStatus.PREPARING) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单当前状态不允许骑手接单");
            }
            stateMachine.transit(from, OrderStatus.RIDER_ACCEPTED);

            int updated = orderMapper.updateStatus(order.getId(), fromStatus,
                    OrderStatus.RIDER_ACCEPTED.getCode());
            if (updated == 0) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更，骑手接单失败");
            }

            // 同步Java对象状态
            order.setStatus(OrderStatus.RIDER_ACCEPTED.getCode());
            if (order.getVersion() != null) {
                order.setVersion(order.getVersion() + 1);
            }

            order.setDeliveryManId(riderId);
            order.setDeliveryTaskId(taskId);
            order.setRiderTakeTime(LocalDateTime.now());
            orderMapper.updateById(order);
            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus,
                    OrderStatus.RIDER_ACCEPTED.getCode(), 3, riderId, "骑手接单");

            // 配送任务状态在deliveryTaskMapper.grab()中已通过乐观锁更新为GRABBED(1)，并设置了grabTime和deliveryManId

            // 通知用户：骑手已接单，正在赶往商家
            sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.RIDER_ACCEPTED.getCode(),
                    3, riderId, "ORDER_RIDER_ACCEPTED", "骑手已接单，正在赶往商家");

            // 通知商家：骑手XXX已接单，正在赶来取餐
            String riderName = (rider.getName() != null && !rider.getName().isBlank()) ? rider.getName() : "骑手";
            sendMerchantNotifyAfterCommit(order, fromStatus, OrderStatus.RIDER_ACCEPTED.getCode(),
                    3, riderId, "ORDER_RIDER_ACCEPTED", riderName + "已接单，正在赶来取餐");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void riderArrive(Long taskId, BigDecimal lng, BigDecimal lat) {
        Long riderId = UserContext.getUserId();
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null || !riderId.equals(task.getDeliveryManId())) {
            throw new BizException(ResultCode.NOT_FOUND, "配送任务不存在");
        }

        // 地理围栏校验：到店打卡需在商家位置附近
        checkGeoFence(lng, lat, task.getMerchantLng(), task.getMerchantLat(),
                GEOFENCE_MERCHANT_RADIUS, "商家");

        // 更新订单状态为RIDER_ARRIVED(11)
        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            int fromStatus = order.getStatus();
            // 校验订单状态必须是RIDER_ACCEPTED(3)
            OrderStatus from = OrderStatus.of(fromStatus);
            if (from != OrderStatus.RIDER_ACCEPTED) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单当前状态不允许骑手到店打卡");
            }
            stateMachine.transit(from, OrderStatus.RIDER_ARRIVED);

            int updated = orderMapper.updateStatus(order.getId(), fromStatus,
                    OrderStatus.RIDER_ARRIVED.getCode());
            if (updated == 0) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更，骑手到店失败");
            }

            // 同步Java对象状态
            order.setStatus(OrderStatus.RIDER_ARRIVED.getCode());
            if (order.getVersion() != null) {
                order.setVersion(order.getVersion() + 1);
            }

            order.setRiderArriveTime(LocalDateTime.now());
            orderMapper.updateById(order);

            // 更新配送任务状态为到店(2)
            deliveryTaskMapper.updateStatus(taskId, DeliveryTaskStatus.ARRIVED_MERCHANT.getCode());
            task.setArriveTime(LocalDateTime.now());
            task.setStatus(DeliveryTaskStatus.ARRIVED_MERCHANT.getCode());
            deliveryTaskMapper.updateById(task);

            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus,
                    OrderStatus.RIDER_ARRIVED.getCode(), 3, riderId, "骑手已到店");

            // 通知用户：骑手已到达商家，等待取餐
            sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.RIDER_ARRIVED.getCode(),
                    3, riderId, "ORDER_RIDER_ARRIVED", "骑手已到达商家，等待取餐");
            // 通知商家：骑手已到店
            sendMerchantNotifyAfterCommit(order, fromStatus, OrderStatus.RIDER_ARRIVED.getCode(),
                    3, riderId, "ORDER_RIDER_ARRIVED", "骑手已到店");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void riderPickup(Long taskId, BigDecimal lng, BigDecimal lat) {
        Long riderId = UserContext.getUserId();
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null || !riderId.equals(task.getDeliveryManId())) {
            throw new BizException(ResultCode.NOT_FOUND, "配送任务不存在");
        }

        // 地理围栏校验：取餐需在商家位置附近
        checkGeoFence(lng, lat, task.getMerchantLng(), task.getMerchantLat(),
                GEOFENCE_MERCHANT_RADIUS, "商家");

        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            int fromStatus = order.getStatus();
            // 校验订单当前状态必须是RIDER_ARRIVED(11)
            OrderStatus from = OrderStatus.of(fromStatus);
            if (from != OrderStatus.RIDER_ARRIVED) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单当前状态不允许骑手取餐");
            }
            stateMachine.transit(from, OrderStatus.DELIVERING);

            // 更新订单状态为DELIVERING(5)
            int updated = orderMapper.updateStatus(order.getId(), fromStatus,
                    OrderStatus.DELIVERING.getCode());
            if (updated == 0) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更，骑手取餐失败");
            }

            // 同步Java对象状态
            order.setStatus(OrderStatus.DELIVERING.getCode());
            if (order.getVersion() != null) {
                order.setVersion(order.getVersion() + 1);
            }

            order.setRiderPickupTime(LocalDateTime.now());
            orderMapper.updateById(order);
            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus,
                    OrderStatus.DELIVERING.getCode(), 3, riderId, "骑手取餐，开始配送");

            // 更新配送任务状态为DELIVERING(4)
            deliveryTaskMapper.updateStatus(taskId, DeliveryTaskStatus.DELIVERING.getCode());
            if (task.getArriveTime() == null) {
                task.setArriveTime(LocalDateTime.now());
            }
            task.setPickupTime(LocalDateTime.now());
            task.setStatus(DeliveryTaskStatus.DELIVERING.getCode());
            deliveryTaskMapper.updateById(task);

            // 通知用户：骑手已取餐，正在为您配送
            sendOrderStatusNotifyAfterCommit(order, fromStatus, OrderStatus.DELIVERING.getCode(),
                    3, riderId, "ORDER_PICKED_UP", "骑手已取餐，正在为您配送");

            // 通知商家：骑手已取餐配送
            sendMerchantNotifyAfterCommit(order, fromStatus, OrderStatus.DELIVERING.getCode(),
                    3, riderId, "ORDER_PICKED_UP", "骑手已取餐配送");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void riderDeliver(Long taskId, BigDecimal lng, BigDecimal lat) {
        Long riderId = UserContext.getUserId();
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null || !riderId.equals(task.getDeliveryManId())) {
            throw new BizException(ResultCode.NOT_FOUND, "配送任务不存在");
        }

        // 地理围栏校验：送达需在用户收货位置附近
        checkGeoFence(lng, lat, task.getUserLng(), task.getUserLat(),
                GEOFENCE_USER_RADIUS, "用户收货地址");

        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            int fromStatus = order.getStatus();
            OrderStatus from = OrderStatus.of(fromStatus);
            stateMachine.transit(from, OrderStatus.DELIVERED);

            int updated = orderMapper.updateStatus(order.getId(), fromStatus,
                    OrderStatus.DELIVERED.getCode());
            if (updated == 0) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更，骑手送达失败");
            }

            // 同步Java对象状态
            order.setStatus(OrderStatus.DELIVERED.getCode());
            if (order.getVersion() != null) {
                order.setVersion(order.getVersion() + 1);
            }

            order.setDeliverTime(LocalDateTime.now());
            orderMapper.updateById(order);
            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus,
                    OrderStatus.DELIVERED.getCode(), 3, riderId, "骑手送达");

            // 更新配送任务状态为已送达(5)
            deliveryTaskMapper.updateStatus(taskId, DeliveryTaskStatus.DELIVERED.getCode());
            task.setDeliverTime(LocalDateTime.now());
            task.setStatus(DeliveryTaskStatus.DELIVERED.getCode());
            deliveryTaskMapper.updateById(task);

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
            // 外卖流程（商家接单时即推送骑手抢单，备餐与骑手赶路并行）：
            // 骑手已接单 → 骑手到店 → 取餐配送 → 已送达
            steps.add(buildStep("骑手已接单", order.getRiderTakeTime(),
                    order.getRiderTakeTime() != null));
            steps.add(buildStep("骑手到店", order.getRiderArriveTime(),
                    order.getRiderArriveTime() != null));
            steps.add(buildStep("配送中", order.getRiderPickupTime(),
                    order.getRiderPickupTime() != null));
            steps.add(buildStep("已送达", order.getDeliverTime(),
                    order.getDeliverTime() != null));
        } else {
            // 堂食/自取：已出餐（待取餐）
            steps.add(buildStep("已出餐", order.getMerchantReadyTime(),
                    order.getMerchantReadyTime() != null));
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
                    // 导航目标判断：配送任务状态为待抢/已抢/到店(0/1/2) → 目标是商家；配送中及以后(>=3) → 目标是用户
                    Integer taskStatus = deliveryTask.getStatus();
                    if (taskStatus != null && taskStatus >= DeliveryTaskStatus.PICKED_UP.getCode()) {
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
            // 回退规格库存（若有规格）
            if (item.getSpecId() != null) {
                dishSpecMapper.rollbackStock(item.getSpecId(), item.getQuantity());
            }
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
        vo.setMerchantReadyTime(order.getMerchantReadyTime());
        vo.setRiderTakeTime(order.getRiderTakeTime());
        vo.setRiderArriveTime(order.getRiderArriveTime());
        vo.setRiderPickupTime(order.getRiderPickupTime());
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
            vo.setMerchantLogo(resolveImageUrl(m.getLogo()));
            vo.setMerchantPhone(m.getContactPhone());
        }

        // 填充用户信息
        User user = null;
        if (order.getUserId() != null) {
            user = userMapper.selectById(order.getUserId());
            if (user != null) {
                vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                vo.setUserPhone(PhoneUtils.mask(user.getPhone()));
                // 骑手端客户信息
                vo.setCustomerName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                vo.setCustomerPhone(PhoneUtils.mask(user.getPhone()));
            }
        }

        // 填充支付状态描述
        if (order.getPayStatus() != null) {
            vo.setPayStatusDesc(order.getPayStatus() == 1 ? "已支付" : "待支付");
        }

        // 填充配送任务相关字段
        DeliveryTask deliveryTask = null;
        if (order.getDeliveryTaskId() != null) {
            deliveryTask = deliveryTaskMapper.selectById(order.getDeliveryTaskId());
        }
        if (deliveryTask == null && order.getStatus() != null && order.getStatus() >= OrderStatus.PREPARING.getCode()) {
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
            // 客户收货地址从配送任务获取
            vo.setCustomerAddress(deliveryTask.getUserAddress());
            // 填充骑手信息
            if (deliveryTask.getDeliveryManId() != null) {
                DeliveryMan rider = deliveryManMapper.selectById(deliveryTask.getDeliveryManId());
                if (rider != null) {
                    vo.setRiderName(rider.getName());
                    vo.setRiderPhone(PhoneUtils.mask(rider.getPhone()));
                }
            }
        } else {
            // 无配送任务（堂食/自取），从地址表获取用户地址
            if (order.getAddressId() != null) {
                Address addrInfo = addressMapper.selectById(order.getAddressId());
                if (addrInfo != null) {
                    String fullAddress = (addrInfo.getProvince() == null ? "" : addrInfo.getProvince())
                            + (addrInfo.getCity() == null ? "" : addrInfo.getCity())
                            + (addrInfo.getDistrict() == null ? "" : addrInfo.getDistrict())
                            + (addrInfo.getDetail() == null ? "" : addrInfo.getDetail());
                    vo.setUserAddress(fullAddress);
                    vo.setCustomerAddress(fullAddress);
                    vo.setUserLng(addrInfo.getLongitude());
                    vo.setUserLat(addrInfo.getLatitude());
                }
            }
        }

        if (withItems) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            vo.setItems(items.stream().map(i -> {
                OrderVO.OrderItemVO iv = new OrderVO.OrderItemVO();
                iv.setId(i.getId());
                iv.setDishId(i.getDishId());
                iv.setDishName(i.getDishName());
                iv.setDishImage(resolveImageUrl(i.getDishImage()));
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

    private String resolveImageUrl(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        String trimmed = key.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
        return storageService.getAccessUrl(trimmed);
    }


    /**
     * 计算配送费（阶梯计费）
     * <p>规则：3公里内基础配送费3元（取商家配置的配送费和3元中的较大值作为基础费），
     * 超出3公里后每增加0.5公里加收1元（不足0.5公里按0.5公里算）。</p>
     *
     * @param baseFee  商家配置的基础配送费
     * @param merchant 商家实体（含经纬度）
     * @param addr     用户收货地址（含经纬度）
     * @return 计算后的配送费
     */
    private BigDecimal calculateDeliveryFee(BigDecimal baseFee, Merchant merchant, Address addr) {
        // 基础配送费：取商家配置和3元的较大值
        BigDecimal base = baseFee.max(new BigDecimal("3"));
        if (merchant.getLongitude() == null || merchant.getLatitude() == null
                || addr.getLongitude() == null || addr.getLatitude() == null) {
            return base;
        }
        // 校验坐标范围（中国境内经度73-135，纬度18-54）
        double mLng = merchant.getLongitude().doubleValue();
        double mLat = merchant.getLatitude().doubleValue();
        double aLng = addr.getLongitude().doubleValue();
        double aLat = addr.getLatitude().doubleValue();
        if (mLng < 73 || mLng > 135 || mLat < 18 || mLat > 54
                || aLng < 73 || aLng > 135 || aLat < 18 || aLat > 54) {
            log.warn("[配送费] 坐标异常，使用基础配送费: merchant({},{}), addr({},{})",
                    mLng, mLat, aLng, aLat);
            return base;
        }
        long distanceMeters = amapService.calculateDistance(mLng, mLat, aLng, aLat);
        if (distanceMeters < 0) {
            // 距离计算失败，返回基础费
            return base;
        }
        // 配送距离上限20km，超过则异常
        if (distanceMeters > 20000) {
            log.warn("[配送费] 配送距离超过20km，使用基础配送费: distance={}m", distanceMeters);
            return base;
        }
        double distanceKm = distanceMeters / 1000.0;
        if (distanceKm <= 3.0) {
            return base;
        }
        double extraKm = distanceKm - 3.0;
        long extraUnits = (long) Math.ceil(extraKm / 0.5);
        // 配送费上限20元
        long maxExtraUnits = 34; // (20-3)/0.5=34
        if (extraUnits > maxExtraUnits) extraUnits = maxExtraUnits;
        BigDecimal extraFee = new BigDecimal(extraUnits); // 每0.5公里1元
        return base.add(extraFee).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 地理围栏校验：骑手操作时必须距离目标点在指定半径内
     *
     * @param riderLng     骑手当前经度（为null时跳过校验，用于管理员/测试/异常场景）
     * @param riderLat     骑手当前纬度
     * @param targetLng    目标点经度（商家或用户位置）
     * @param targetLat    目标点纬度
     * @param radiusMeters 允许的半径（米）
     * @param targetName   目标点名称，用于错误提示
     */
    private void checkGeoFence(BigDecimal riderLng, BigDecimal riderLat,
                               BigDecimal targetLng, BigDecimal targetLat,
                               int radiusMeters, String targetName) {
        // ===== 课设展示模式：临时跳过地理围栏校验，直接放行 =====
        // 展示结束后将 SKIP_GEOFENCE 改回 false 即可恢复
        if (SKIP_GEOFENCE) {
            log.warn("[地理围栏] 课设展示模式：跳过 {} 围栏校验", targetName);
            return;
        }
        // 目标点坐标缺失：历史数据/未配置坐标，记录警告但放行（无法做围栏校验，避免阻塞业务）
        if (targetLng == null || targetLat == null) {
            log.warn("[地理围栏] {}坐标缺失，跳过校验: target=({}, {})", targetName, targetLng, targetLat);
            return;
        }
        // 骑手未传GPS：强制要求前端必须传递，避免绕过围栏
        if (riderLng == null || riderLat == null) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "请开启位置权限后重试（需要获取您的当前位置）");
        }
        // 中国大致经纬度范围校验（排除异常值）
        double rLng = riderLng.doubleValue();
        double rLat = riderLat.doubleValue();
        double tLng = targetLng.doubleValue();
        double tLat = targetLat.doubleValue();
        if (rLng < 73 || rLng > 135 || rLat < 18 || rLat > 54
                || tLng < 73 || tLng > 135 || tLat < 18 || tLat > 54) {
            log.warn("[地理围栏] 坐标异常: rider=({},{}), target=({}, {})", rLng, rLat, tLng, tLat);
            throw new BizException(ResultCode.PARAM_ERROR, "位置信息异常，请重新定位后重试");
        }

        double distance = haversineDistance(rLat, rLng, tLat, tLng);
        if (distance > radiusMeters) {
            log.warn("[地理围栏] 骑手距离{}过远: distance={}m, radius={}m, rider=({},{}), target=({},{})",
                    targetName, (int) distance, radiusMeters, rLng, rLat, tLng, tLat);
            throw new BizException(ResultCode.FAIL,
                    String.format("您距离%s还有%d米，请在%d米范围内操作",
                            targetName, (int) distance, radiusMeters));
        }
        log.debug("[地理围栏] 校验通过: target={}, distance={}m", targetName, (int) distance);
    }

    /**
     * Haversine公式计算两点间球面距离（米）
     */
    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000.0;
        double toRad = Math.PI / 180.0;
        double dLat = (lat2 - lat1) * toRad;
        double dLng = (lng2 - lng1) * toRad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1 * toRad) * Math.cos(lat2 * toRad)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }
}
