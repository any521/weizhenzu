package com.weizhenzu.api.controller.rider;

import com.weizhenzu.application.service.OrderService;
import com.weizhenzu.application.service.RiderService;
import com.weizhenzu.common.annotation.RequireLogin;
import com.weizhenzu.common.enums.UserTypeEnum;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.entity.DeliveryTask;
import com.weizhenzu.domain.vo.DeliveryTrackingVO;
import com.weizhenzu.domain.vo.OrderVO;
import com.weizhenzu.domain.vo.RiderIncomeVO;
import com.weizhenzu.infrastructure.persistence.mapper.DeliveryTaskMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 骑手端订单 Controller（我的订单、订单操作、钱包）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "骑手端-订单", description = "骑手订单管理接口")
@RestController
@RequiredArgsConstructor
@RequireLogin(UserTypeEnum.RIDER)
public class RiderOrderController {

    private final OrderService orderService;
    private final RiderService riderService;
    private final DeliveryTaskMapper deliveryTaskMapper;

    @Operation(summary = "钱包信息")
    @GetMapping("/api/rider/wallet")
    public Result<RiderIncomeVO> wallet() {
        return Result.ok(riderService.incomeStatistics());
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/api/rider/orders")
    public Result<PageResult<OrderVO>> page(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer status) {
        // 兼容前端传 page/pageSize 或 current/size
        int pageNum = current != null ? current : (page != null ? page : 1);
        int pageSizeNum = size != null ? size : (pageSize != null ? pageSize : 10);
        return Result.ok(orderService.riderPage(pageNum, pageSizeNum, status));
    }

    @Operation(summary = "订单详情")
    @GetMapping({"/api/rider/orders/{id}", "/api/rider/tasks/{id}"})
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @Operation(summary = "抢单")
    @PostMapping({"/api/rider/orders/tasks/{taskId}/grab", "/api/rider/tasks/{taskId}/grab"})
    public Result<Void> grab(@PathVariable Long taskId) {
        orderService.riderGrab(taskId);
        return Result.ok();
    }

    @Operation(summary = "到店（需传入骑手当前GPS坐标做地理围栏校验）")
    @PostMapping({"/api/rider/orders/tasks/{taskId}/arrive", "/api/rider/tasks/{taskId}/arrive"})
    public Result<Void> arrive(
            @PathVariable Long taskId,
            @RequestParam(required = false) BigDecimal lng,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude) {
        BigDecimal finalLng = lng != null ? lng : longitude;
        BigDecimal finalLat = lat != null ? lat : latitude;
        orderService.riderArrive(taskId, finalLng, finalLat);
        return Result.ok();
    }

    @Operation(summary = "取餐（需传入骑手当前GPS坐标做地理围栏校验）")
    @PostMapping({"/api/rider/orders/tasks/{taskId}/pickup", "/api/rider/tasks/{taskId}/pickup"})
    public Result<Void> pickup(
            @PathVariable Long taskId,
            @RequestParam(required = false) BigDecimal lng,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude) {
        BigDecimal finalLng = lng != null ? lng : longitude;
        BigDecimal finalLat = lat != null ? lat : latitude;
        orderService.riderPickup(taskId, finalLng, finalLat);
        return Result.ok();
    }

    @Operation(summary = "送达（需传入骑手当前GPS坐标做地理围栏校验）")
    @PostMapping({"/api/rider/orders/tasks/{taskId}/deliver", "/api/rider/tasks/{taskId}/deliver",
            "/api/rider/tasks/{taskId}/complete"})
    public Result<Void> deliver(
            @PathVariable Long taskId,
            @RequestParam(required = false) BigDecimal lng,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude) {
        BigDecimal finalLng = lng != null ? lng : longitude;
        BigDecimal finalLat = lat != null ? lat : latitude;
        orderService.riderDeliver(taskId, finalLng, finalLat);
        return Result.ok();
    }

    @Operation(summary = "订单追踪")
    @GetMapping({"/api/rider/orders/tasks/{taskId}/tracking", "/api/rider/tasks/{taskId}/tracking"})
    public Result<DeliveryTrackingVO> tracking(@PathVariable Long taskId) {
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null) {
            return Result.ok(null);
        }
        return Result.ok(orderService.tracking(task.getOrderId()));
    }
}
