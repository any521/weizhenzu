package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.OrderService;
import com.weizhenzu.common.annotation.Idempotent;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.OrderCancelDTO;
import com.weizhenzu.domain.dto.OrderCreateDTO;
import com.weizhenzu.domain.vo.DeliveryTrackingVO;
import com.weizhenzu.domain.vo.OrderCreateVO;
import com.weizhenzu.domain.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端订单 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "C端-订单", description = "用户订单相关接口")
@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @Idempotent(key = "#dto.clientToken", expire = 30, message = "请勿重复提交订单")
    @PostMapping
    public Result<OrderCreateVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.ok(orderService.createOrder(dto));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping
    public Result<PageResult<OrderVO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.userPage(current, size, status));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @Valid @RequestBody OrderCancelDTO dto) {
        orderService.cancel(id, dto);
        return Result.ok();
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirmReceived(id);
        return Result.ok();
    }

    @Operation(summary = "配送跟踪")
    @GetMapping("/{id}/delivery")
    public Result<DeliveryTrackingVO> tracking(@PathVariable Long id) {
        return Result.ok(orderService.tracking(id));
    }
}
