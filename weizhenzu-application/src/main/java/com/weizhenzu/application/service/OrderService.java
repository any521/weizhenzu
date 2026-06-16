package com.weizhenzu.application.service;

import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.domain.dto.OrderCancelDTO;
import com.weizhenzu.domain.dto.OrderCreateDTO;
import com.weizhenzu.domain.vo.DeliveryTrackingVO;
import com.weizhenzu.domain.vo.OrderCreateVO;
import com.weizhenzu.domain.vo.OrderVO;

/**
 * 订单服务接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface OrderService {

    /**
     * 创建订单（C端）
     */
    OrderCreateVO createOrder(OrderCreateDTO dto);

    /**
     * 订单详情
     */
    OrderVO detail(Long orderId);

    /**
     * 我的订单列表（C端）
     */
    PageResult<OrderVO> userPage(Integer current, Integer size, Integer status);

    /**
     * 商家订单列表
     */
    PageResult<OrderVO> merchantPage(Integer current, Integer size, Integer status);

    /**
     * 骑手订单列表
     */
    PageResult<OrderVO> riderPage(Integer current, Integer size, Integer status);

    /**
     * 取消订单（C端）
     */
    void cancel(Long orderId, OrderCancelDTO dto);

    /**
     * 确认收货（C端）
     */
    void confirmReceived(Long orderId);

    /**
     * 商家接单
     */
    void merchantAccept(Long orderId);

    /**
     * 商家拒单
     */
    void merchantReject(Long orderId, String reason);

    /**
     * 商家出餐完成
     */
    void merchantReady(Long orderId);

    /**
     * 骑手抢单
     */
    void riderGrab(Long taskId);

    /**
     * 骑手到店
     */
    void riderArrive(Long taskId);

    /**
     * 骑手取餐
     */
    void riderPickup(Long taskId);

    /**
     * 骑手送达
     */
    void riderDeliver(Long taskId);

    /**
     * 配送跟踪
     */
    DeliveryTrackingVO tracking(Long orderId);
}
