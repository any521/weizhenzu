package com.weizhenzu.api.controller.rider;

import com.weizhenzu.application.service.OrderService;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 骑手端订单 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "骑手端-订单", description = "骑手订单管理接口")
@RestController
@RequestMapping("/api/rider/orders")
@RequiredArgsConstructor
public class RiderOrderController {

    private final OrderService orderService;

    @Operation(summary = "我的订单列表")
    @GetMapping
    public Result<PageResult<OrderVO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.riderPage(current, size, status));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @Operation(summary = "抢单")
    @PostMapping("/tasks/{taskId}/grab")
    public Result<Void> grab(@PathVariable Long taskId) {
        orderService.riderGrab(taskId);
        return Result.ok();
    }

    @Operation(summary = "到店")
    @PostMapping("/tasks/{taskId}/arrive")
    public Result<Void> arrive(@PathVariable Long taskId) {
        orderService.riderArrive(taskId);
        return Result.ok();
    }

    @Operation(summary = "取餐")
    @PostMapping("/tasks/{taskId}/pickup")
    public Result<Void> pickup(@PathVariable Long taskId) {
        orderService.riderPickup(taskId);
        return Result.ok();
    }

    @Operation(summary = "送达")
    @PostMapping("/tasks/{taskId}/deliver")
    public Result<Void> deliver(@PathVariable Long taskId) {
        orderService.riderDeliver(taskId);
        return Result.ok();
    }
}
