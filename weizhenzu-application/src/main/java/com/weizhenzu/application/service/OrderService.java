package com.weizhenzu.application.service;

import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.domain.dto.OrderCancelDTO;
import com.weizhenzu.domain.dto.OrderCreateDTO;
import com.weizhenzu.domain.dto.OrderPreviewDTO;
import com.weizhenzu.domain.vo.DeliveryTrackingVO;
import com.weizhenzu.domain.vo.OrderCreateVO;
import com.weizhenzu.domain.vo.OrderPreviewVO;
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
     * 订单预览（C端）：不创建订单，仅按地址计算金额明细供前端展示
     */
    OrderPreviewVO previewOrder(OrderPreviewDTO dto);

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
     * 骑手到店（需传入当前GPS坐标做地理围栏校验）
     * @param lng 骑手当前经度，可为null（null时跳过围栏校验，仅管理员/测试使用）
     * @param lat 骑手当前纬度
     */
    void riderArrive(Long taskId, java.math.BigDecimal lng, java.math.BigDecimal lat);

    /**
     * 骑手取餐（需传入当前GPS坐标做地理围栏校验）
     */
    void riderPickup(Long taskId, java.math.BigDecimal lng, java.math.BigDecimal lat);

    /**
     * 骑手送达（需传入当前GPS坐标做地理围栏校验）
     */
    void riderDeliver(Long taskId, java.math.BigDecimal lng, java.math.BigDecimal lat);

    /**
     * 配送跟踪
     */
    DeliveryTrackingVO tracking(Long orderId);

    /**
     * 骑手发送留言
     * @param taskId 配送任务ID
     * @param content 留言内容
     */
    void riderSendMessage(Long taskId, String content);

    /**
     * 自动取消超时未支付的订单（定时任务调用）
     * @param timeoutMinutes 超时分钟数
     * @return 取消的订单数量
     */
    int autoCancelTimeoutOrders(int timeoutMinutes);

    /**
     * 处理单条超时未支付订单（独立事务REQUIRES_NEW，逐条调用避免跨事务不一致）
     * 仅供 autoCancelTimeoutOrders 内部通过self代理调用
     * @return true=取消成功，false=跳过（状态已变更）
     */
    boolean cancelSingleTimeoutOrder(Long orderId);

    /**
     * 处理单条商家超时未接单订单（独立事务REQUIRES_NEW，逐条调用避免跨事务不一致）
     * 1. 乐观锁更新订单 PENDING_ACCEPT→REFUNDING
     * 2. 创建退款单+调用第三方退款
     * 3. 成功则更新订单/支付/退款状态为已退款，失败则保持REFUNDING并告警
     * @return true=退款成功，false=跳过（状态已变更）或退款失败
     */
    boolean processMerchantTimeoutOrder(Long orderId);
}
