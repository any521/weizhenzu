package com.weizhenzu.api.controller.merchant;

import com.weizhenzu.application.service.OrderService;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.domain.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * B端订单 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "B端-订单", description = "商家订单管理接口")
@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderService orderService;

    @Operation(summary = "商家订单列表")
    @GetMapping
    public Result<PageResult<OrderVO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.merchantPage(current, size, status));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @Operation(summary = "接单")
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        orderService.merchantAccept(id);
        return Result.ok();
    }

    @Operation(summary = "拒单")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.merchantReject(id, body.get("reason"));
        return Result.ok();
    }

    @Operation(summary = "出餐完成")
    @PostMapping("/{id}/ready")
    public Result<Void> ready(@PathVariable Long id) {
        orderService.merchantReady(id);
        return Result.ok();
    }

    @Operation(summary = "更新订单状态")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        // 根据状态码分发到对应的业务方法
        switch (status) {
            case 2 -> // MERCHANT_ACCEPTED - 商家接单
                orderService.merchantAccept(id);
            case 8 -> // CANCELED - 商家拒单（需要前端传reason，这里如果没有reason给个默认值）
                orderService.merchantReject(id, "商家取消订单");
            default -> throw new BizException(ResultCode.PARAM_ERROR, "不支持的状态更新: " + status);
        }
        return Result.ok();
    }
}
