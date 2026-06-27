package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weizhenzu.application.service.ReviewService;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.domain.dto.OrderNotifyMessage;
import com.weizhenzu.domain.dto.ReviewCreateDTO;
import com.weizhenzu.domain.entity.DeliveryMan;
import com.weizhenzu.domain.entity.Merchant;
import com.weizhenzu.domain.entity.Order;
import com.weizhenzu.domain.entity.OrderItem;
import com.weizhenzu.domain.entity.Review;
import com.weizhenzu.domain.entity.User;
import com.weizhenzu.domain.vo.ReviewVO;
import com.weizhenzu.infrastructure.persistence.mapper.MerchantMapper;
import com.weizhenzu.infrastructure.persistence.mapper.DeliveryManMapper;
import com.weizhenzu.infrastructure.persistence.mapper.OrderItemMapper;
import com.weizhenzu.infrastructure.persistence.mapper.OrderMapper;
import com.weizhenzu.infrastructure.persistence.mapper.ReviewMapper;
import com.weizhenzu.infrastructure.persistence.mapper.UserMapper;
import com.weizhenzu.infrastructure.mq.OrderNotifyProducer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价服务实现
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final DeliveryManMapper deliveryManMapper;
    private final ObjectMapper objectMapper;
    private final OrderNotifyProducer orderNotifyProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ReviewCreateDTO dto) {
        Long userId = UserContext.getUserId();
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(order.getIsRated())) {
            throw new BizException(ResultCode.PARAM_ERROR, "订单已评价");
        }
        // 只有已送达(6)或已完成(7)的订单才能评价
        if (order.getStatus() == null ||
                (order.getStatus() != 6 && order.getStatus() != 7)) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单尚未送达，无法评价");
        }

        Review r = new Review();
        r.setOrderId(order.getId());
        r.setOrderNo(order.getOrderNo());
        r.setUserId(userId);
        r.setMerchantId(order.getMerchantId());
        r.setDeliveryManId(order.getDeliveryManId());
        r.setRating(dto.getRating());
        r.setTasteScore(dto.getTasteScore());
        r.setPackingScore(dto.getPackingScore());
        r.setDeliveryScore(dto.getDeliveryScore());
        r.setContent(dto.getContent());
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            r.setImages(toJson(dto.getImages()));
        }
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            r.setTags(toJson(dto.getTags()));
        }
        r.setAnonymous(dto.getAnonymous() == null ? 0 : dto.getAnonymous());
        r.setStatus(1);
        reviewMapper.insert(r);

        // 标记订单已评价
        order.setIsRated(1);
        orderMapper.updateById(order);

        // 更新商家评分（简单平均）
        try {
            updateMerchantRating(order.getMerchantId());
        } catch (Exception ignored) {}

        // 更新骑手评分（如果有骑手）
        if (order.getDeliveryManId() != null) {
            try {
                updateRiderRating(order.getDeliveryManId());
            } catch (Exception ignored) {}
        }

        return r.getId();
    }

    private void updateMerchantRating(Long merchantId) {
        if (merchantId == null) return;
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<>();
        qw.eq(Review::getMerchantId, merchantId).eq(Review::getStatus, 1);
        List<Review> reviews = reviewMapper.selectList(qw);
        if (reviews.isEmpty()) return;
        double avg = reviews.stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(Review::getRating)
                .average().orElse(5.0);
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant != null) {
            merchant.setRating(java.math.BigDecimal.valueOf(Math.round(avg * 10) / 10.0));
            merchantMapper.updateById(merchant);
        }
    }

    private void updateRiderRating(Long riderId) {
        if (riderId == null) return;
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<>();
        qw.eq(Review::getDeliveryManId, riderId).eq(Review::getStatus, 1);
        List<Review> reviews = reviewMapper.selectList(qw);
        if (reviews.isEmpty()) return;
        double avg = reviews.stream()
                .filter(r -> r.getDeliveryScore() != null)
                .mapToInt(Review::getDeliveryScore)
                .average().orElse(5.0);
        DeliveryMan dm = deliveryManMapper.selectById(riderId);
        if (dm != null) {
            dm.setRating(java.math.BigDecimal.valueOf(Math.round(avg * 10) / 10.0));
            deliveryManMapper.updateById(dm);
        }
    }

    @Override
    public ReviewVO detail(Long id) {
        Review r = reviewMapper.selectById(id);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "评价不存在");
        }
        return toVO(r);
    }

    @Override
    public PageResult<ReviewVO> merchantPage(Integer current, Integer size, Integer rating) {
        Long merchantId = UserContext.getUserId();
        Page<Review> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(Review::getMerchantId, merchantId)
                .eq(rating != null, Review::getRating, rating)
                .orderByDesc(Review::getCreatedAt);
        Page<Review> result = reviewMapper.selectPage(page, wrapper);
        List<ReviewVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<ReviewVO> userPage(Integer current, Integer size) {
        Long userId = UserContext.getUserId();
        Page<Review> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .orderByDesc(Review::getCreatedAt);
        Page<Review> result = reviewMapper.selectPage(page, wrapper);
        List<ReviewVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<ReviewVO> merchantReviews(Long merchantId, Integer current, Integer size) {
        Page<Review> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(Review::getMerchantId, merchantId)
                .eq(Review::getStatus, 1)
                .orderByDesc(Review::getCreatedAt);
        Page<Review> result = reviewMapper.selectPage(page, wrapper);
        List<ReviewVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<ReviewVO> dishReviews(Long dishId, Integer current, Integer size) {
        // 简化实现：查询所有公开评价（菜品与评价的关联通过订单项建立，此处返回最新评价列表）
        Page<Review> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(Review::getStatus, 1)
                .orderByDesc(Review::getCreatedAt);
        Page<Review> result = reviewMapper.selectPage(page, wrapper);
        List<ReviewVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void reply(Long id, String content) {
        Long merchantId = UserContext.getUserId();
        Review r = reviewMapper.selectById(id);
        if (r == null || !merchantId.equals(r.getMerchantId())) {
            throw new BizException(ResultCode.NOT_FOUND, "评价不存在");
        }
        r.setMerchantReply(content);
        r.setMerchantReplyTime(LocalDateTime.now());
        reviewMapper.updateById(r);

        // 商家回复后，通过 WebSocket 推送通知给评价对应的用户
        try {
            OrderNotifyMessage msg = OrderNotifyMessage.builder()
                    .msgId(r.getOrderId() + ":REVIEW_REPLY:" + System.currentTimeMillis())
                    .orderId(r.getOrderId())
                    .orderNo(r.getOrderNo())
                    .userId(r.getUserId())
                    .merchantId(r.getMerchantId())
                    .type("REVIEW_REPLY")
                    .content("商家已回复您的评价：" + (content.length() > 30 ? content.substring(0, 30) + "..." : content))
                    .operatorType(2)
                    .operatorId(merchantId)
                    .eventTime(LocalDateTime.now())
                    .build();
            orderNotifyProducer.sendOrderStatusToUser(msg);
        } catch (Exception e) {
            log.error("[评价回复] 推送通知失败: reviewId={}, orderId={}", id, r.getOrderId(), e);
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Long merchantId = UserContext.getUserId();
        Review r = reviewMapper.selectById(id);
        if (r == null || !merchantId.equals(r.getMerchantId())) {
            throw new BizException(ResultCode.NOT_FOUND, "评价不存在");
        }
        Review update = new Review();
        update.setId(id);
        update.setStatus(status);
        reviewMapper.updateById(update);
    }

    private ReviewVO toVO(Review r) {
        ReviewVO vo = new ReviewVO();
        vo.setId(r.getId());
        vo.setOrderId(r.getOrderId());
        vo.setOrderNo(r.getOrderNo());
        vo.setUserId(r.getUserId());
        vo.setMerchantId(r.getMerchantId());
        vo.setDeliveryManId(r.getDeliveryManId());
        vo.setRating(r.getRating());
        vo.setTasteScore(r.getTasteScore());
        vo.setPackingScore(r.getPackingScore());
        vo.setDeliveryScore(r.getDeliveryScore());
        vo.setContent(r.getContent());
        vo.setImages(parseJsonList(r.getImages()));
        vo.setTags(parseJsonList(r.getTags()));
        vo.setAnonymous(r.getAnonymous());
        vo.setMerchantReply(r.getMerchantReply());
        vo.setReply(r.getMerchantReply());
        vo.setMerchantReplyTime(r.getMerchantReplyTime());
        vo.setStatus(r.getStatus());
        vo.setCreatedAt(r.getCreatedAt());

        // 初始化默认值
        vo.setDishNames(Collections.emptyList());
        vo.setMerchantName("未知商家");
        vo.setDeliveryManName("平台配送");

        // 用户信息（匿名时隐藏）
        if (r.getAnonymous() != null && r.getAnonymous() == 1) {
            vo.setUserNickname("匿名用户");
        } else {
            User u = userMapper.selectById(r.getUserId());
            if (u != null) {
                vo.setUserNickname(u.getNickname() != null ? u.getNickname() : (u.getUsername() != null ? u.getUsername() : "匿名用户"));
                vo.setUserAvatar(u.getAvatar());
            } else {
                vo.setUserNickname("匿名用户");
            }
        }

        // 商家名称
        if (r.getMerchantId() != null) {
            Merchant m = merchantMapper.selectById(r.getMerchantId());
            if (m != null && m.getName() != null) {
                vo.setMerchantName(m.getName());
            }
        }

        // 骑手名称
        if (r.getDeliveryManId() != null) {
            DeliveryMan dm = deliveryManMapper.selectById(r.getDeliveryManId());
            if (dm != null && dm.getName() != null) {
                vo.setDeliveryManName(dm.getName());
            }
        }

        // 评价菜品名称列表
        if (r.getOrderId() != null) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, r.getOrderId()));
            List<String> dishNames = items.stream()
                    .map(OrderItem::getDishName)
                    .filter(name -> name != null && !name.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            vo.setDishNames(dishNames);
        }

        return vo;
    }

    /**
     * List 转 JSON 字符串
     */
    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("[Review] 序列化 JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 JSON 字符串为 List
     */
    private List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("[Review] 解析 JSON 失败: json={}, error={}", json, e.getMessage());
            if (json.contains(",")) {
                return List.of(json.split(","));
            }
            return List.of(json);
        }
    }
}
