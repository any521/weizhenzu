package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weizhenzu.application.service.OrderService;
import com.weizhenzu.application.statemachine.OrderStateMachine;
import com.weizhenzu.common.annotation.DistributedLock;
import com.weizhenzu.common.annotation.Idempotent;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.common.utils.OrderNoGenerator;
import com.weizhenzu.common.utils.PhoneUtils;
import com.weizhenzu.domain.dto.OrderCancelDTO;
import com.weizhenzu.domain.dto.OrderCreateDTO;
import com.weizhenzu.domain.entity.*;
import com.weizhenzu.domain.enums.OrderStatus;
import com.weizhenzu.domain.vo.DeliveryTrackingVO;
import com.weizhenzu.domain.vo.OrderCreateVO;
import com.weizhenzu.domain.vo.OrderVO;
import com.weizhenzu.infrastructure.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final OrderStateMachine stateMachine;

    @Idempotent(key = "#dto.clientToken", expire = 30, message = "请勿重复提交订单")
    @DistributedLock(key = "'order:create:' + #dto.merchantId + ':' + T(com.weizhenzu.common.context.UserContext).getUserId()")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderCreateVO createOrder(OrderCreateDTO dto) {
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
        BigDecimal payAmount = total.add(packingFee).add(deliveryFee);

        // 5. 创建订单主表
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
        order.setCouponAmount(BigDecimal.ZERO);
        order.setPayAmount(payAmount);
        order.setRemark(dto.getRemark());
        order.setIsRated(0);
        order.setSource(1);
        order.setExpectedTime(LocalDateTime.now().plusMinutes(45));
        orderMapper.insert(order);

        // 6. 创建订单明细
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            orderItemMapper.insert(item);
        }

        // 7. 记录订单日志
        saveOrderLog(order.getId(), order.getOrderNo(), null, OrderStatus.PENDING_PAY.getCode(),
                1, userId, "用户创建订单");

        // 8. 返回
        OrderCreateVO vo = new OrderCreateVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.PENDING_PAY.getDesc());
        vo.setPayAmount(payAmount);
        vo.setExpireTime(LocalDateTime.now().plusMinutes(15));
        return vo;
    }

    @Override
    public OrderVO detail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
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
                .map(o -> toVO(o, false))
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

        // 回退库存
        rollbackStock(orderId);

        // 记录日志
        saveOrderLog(orderId, order.getOrderNo(), order.getStatus(),
                OrderStatus.CANCELED.getCode(), 1, userId, dto.getReason());
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
        saveOrderLog(orderId, order.getOrderNo(), order.getStatus(),
                OrderStatus.MERCHANT_ACCEPTED.getCode(), 2, merchantId, "商家接单");

        // 创建配送任务
        createDeliveryTask(order);
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
        rollbackStock(orderId);
        saveOrderLog(orderId, order.getOrderNo(), order.getStatus(),
                OrderStatus.CANCELED.getCode(), 2, merchantId, "商家拒单：" + reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantReady(Long orderId) {
        Long merchantId = UserContext.getUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !merchantId.equals(order.getMerchantId())) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        // 商家出餐完成 -> 等待骑手取餐，状态保持 MERCHANT_ACCEPTED
        saveOrderLog(orderId, order.getOrderNo(), order.getStatus(),
                order.getStatus(), 2, merchantId, "商家出餐完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void riderGrab(Long taskId) {
        Long riderId = UserContext.getUserId();
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ResultCode.NOT_FOUND, "配送任务不存在");
        }
        int rows = deliveryTaskMapper.grab(taskId, riderId);
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "任务已被抢或已取消");
        }
        // 更新订单状态
        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            orderMapper.updateStatus(order.getId(), order.getStatus(),
                    OrderStatus.RIDER_TAKEN.getCode());
            order.setDeliveryManId(riderId);
            order.setDeliveryTaskId(taskId);
            orderMapper.updateById(order);
            saveOrderLog(order.getId(), order.getOrderNo(), order.getStatus(),
                    OrderStatus.RIDER_TAKEN.getCode(), 3, riderId, "骑手接单");
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
        deliveryTaskMapper.updateStatus(taskId, 2);
        task.setArriveTime(LocalDateTime.now());
        deliveryTaskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void riderPickup(Long taskId) {
        Long riderId = UserContext.getUserId();
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null || !riderId.equals(task.getDeliveryManId())) {
            throw new BizException(ResultCode.NOT_FOUND, "配送任务不存在");
        }
        deliveryTaskMapper.updateStatus(taskId, 3);
        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            orderMapper.updateStatus(order.getId(), order.getStatus(),
                    OrderStatus.PICKED_UP.getCode());
            saveOrderLog(order.getId(), order.getOrderNo(), order.getStatus(),
                    OrderStatus.PICKED_UP.getCode(), 3, riderId, "骑手取餐");
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
        deliveryTaskMapper.updateStatus(taskId, 5);
        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            orderMapper.updateStatus(order.getId(), order.getStatus(),
                    OrderStatus.DELIVERED.getCode());
            saveOrderLog(order.getId(), order.getOrderNo(), order.getStatus(),
                    OrderStatus.DELIVERED.getCode(), 3, riderId, "骑手送达");
        }
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

        // 构建步骤
        List<DeliveryTrackingVO.Step> steps = new ArrayList<>();
        steps.add(buildStep("已下单", order.getCreatedAt(), true));
        steps.add(buildStep("商家接单", order.getMerchantAcceptTime(),
                order.getMerchantAcceptTime() != null));
        steps.add(buildStep("骑手取餐", order.getRiderTakeTime(),
                order.getRiderTakeTime() != null));
        steps.add(buildStep("配送中", order.getDeliverTime(),
                order.getDeliverTime() != null));
        steps.add(buildStep("已送达", order.getCompleteTime(),
                order.getCompleteTime() != null));
        vo.setSteps(steps);

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
            vo.setMerchant(mi);
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
        DeliveryTask task = new DeliveryTask();
        task.setTaskNo(OrderNoGenerator.taskNo());
        task.setOrderId(order.getId());
        task.setOrderNo(order.getOrderNo());
        task.setMerchantId(order.getMerchantId());
        task.setMerchantName(m == null ? null : m.getName());
        task.setMerchantAddress(m == null ? null : m.getAddress());
        task.setMerchantLng(m == null ? null : m.getLongitude());
        task.setMerchantLat(m == null ? null : m.getLatitude());
        task.setStatus(0);
        task.setFee(order.getDeliveryFee());
        deliveryTaskMapper.insert(task);
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
        vo.setCreatedAt(order.getCreatedAt());

        Merchant m = merchantMapper.selectById(order.getMerchantId());
        if (m != null) {
            vo.setMerchantName(m.getName());
            vo.setMerchantLogo(m.getLogo());
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
