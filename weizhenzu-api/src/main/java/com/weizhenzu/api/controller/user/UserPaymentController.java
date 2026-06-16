package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.PaymentService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.PayDTO;
import com.weizhenzu.domain.vo.PaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/user/payment")
@RequiredArgsConstructor
public class UserPaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "创建支付")
    @PostMapping
    public Result<PaymentVO> create(@Valid @RequestBody PayDTO dto) {
        return Result.ok(paymentService.createPayment(dto));
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/order/{orderId}")
    public Result<PaymentVO> query(@PathVariable Long orderId) {
        return Result.ok(paymentService.queryPayment(orderId));
    }
}
