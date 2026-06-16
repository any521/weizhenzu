package com.weizhenzu.api.controller.publicapi;

import com.weizhenzu.application.service.PaymentService;
import com.weizhenzu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 公共接口 Controller（支付回调等）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "公共接口", description = "支付回调等公共接口")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PaymentService paymentService;

    @Operation(summary = "支付宝支付回调")
    @PostMapping("/payment/alipay/callback")
    public String alipayCallback(HttpServletRequest request) {
        String paymentNo = request.getParameter("out_trade_no");
        String thirdPartyNo = request.getParameter("trade_no");
        String status = request.getParameter("trade_status");
        log.info("[支付宝回调] paymentNo={}, tradeNo={}, status={}", paymentNo, thirdPartyNo, status);
        boolean success = "TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status);
        return paymentService.handleCallback(paymentNo, thirdPartyNo, success);
    }

    @Operation(summary = "微信支付回调")
    @PostMapping("/payment/wechat/callback")
    public Map<String, Object> wechatCallback(@RequestBody String body) {
        log.info("[微信回调] body={}", body);
        // Mock 实现：直接返回成功
        Map<String, Object> result = new HashMap<>();
        result.put("code", "SUCCESS");
        result.put("message", "成功");
        return result;
    }

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("ok");
    }
}
