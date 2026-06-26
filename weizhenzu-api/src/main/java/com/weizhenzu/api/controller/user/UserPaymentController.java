package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.PaymentService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.PayDTO;
import com.weizhenzu.domain.dto.RefundApplyDTO;
import com.weizhenzu.domain.vo.PaymentVO;
import com.weizhenzu.domain.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端支付 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "C端-支付", description = "支付相关接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserPaymentController {

    private final PaymentService paymentService;

    // ========== 原有端点（保持兼容） ==========

    @Operation(summary = "创建支付")
    @PostMapping("/payment")
    public Result<PaymentVO> create(@Valid @RequestBody PayDTO dto) {
        return Result.ok(paymentService.createPayment(dto));
    }

    @Operation(summary = "查询支付状态（DB）")
    @GetMapping("/payment/order/{orderId}")
    public Result<PaymentVO> query(@PathVariable Long orderId) {
        return Result.ok(paymentService.queryPayment(orderId));
    }

    @Operation(summary = "主动查询支付状态（同步第三方）")
    @GetMapping("/payment/order/{orderId}/status")
    public Result<PaymentVO> queryStatus(@PathVariable Long orderId) {
        return Result.ok(paymentService.queryPaymentStatus(orderId));
    }

    @Operation(summary = "申请退款")
    @PostMapping("/payment/order/{orderId}/refund")
    public Result<RefundVO> refund(@PathVariable Long orderId,
                                   @Valid @RequestBody RefundApplyDTO dto) {
        return Result.ok(paymentService.refund(orderId, dto));
    }

    // ========== RESTful 端点（匹配前端调用） ==========

    @Operation(summary = "创建支付（RESTful：指定订单下单）")
    @PostMapping("/payments/orders/{orderId}")
    public Result<PaymentVO> createForOrder(@PathVariable Long orderId,
                                            @Valid @RequestBody CreatePayRequest req) {
        PayDTO dto = new PayDTO();
        dto.setOrderId(orderId);
        dto.setPayType(req.getPayType());
        dto.setClient("H5");
        return Result.ok(paymentService.createPayment(dto));
    }

    @Operation(summary = "查询支付单状态（按支付单号）")
    @GetMapping("/payments/{paymentNo}")
    public Result<PaymentVO> getPayment(@PathVariable String paymentNo,
                                        @RequestParam(required = false, defaultValue = "false") boolean sync) {
        if (sync) {
            return Result.ok(paymentService.queryPaymentStatusByNo(paymentNo));
        }
        return Result.ok(paymentService.queryPaymentByNo(paymentNo));
    }

    @Operation(summary = "取消/关闭支付单")
    @PostMapping("/payments/{paymentNo}/cancel")
    public Result<Void> cancelPayment(@PathVariable String paymentNo) {
        paymentService.closePaymentByNo(paymentNo);
        return Result.ok();
    }

    /**
     * 创建支付请求体（RESTful 风格，orderId 在路径中）
     */
    @Data
    public static class CreatePayRequest {
        @NotNull(message = "支付方式不能为空")
        private Integer payType;
        private String client;
    }
}
