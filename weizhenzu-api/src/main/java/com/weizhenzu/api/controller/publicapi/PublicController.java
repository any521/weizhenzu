package com.weizhenzu.api.controller.publicapi;

import com.weizhenzu.application.service.PaymentService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.infrastructure.thirdparty.amap.AmapService;
import com.weizhenzu.infrastructure.thirdparty.amap.dto.RegeoResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 公共接口 Controller（支付回调、地理服务等）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "公共接口", description = "支付回调、地理服务等公共接口")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PaymentService paymentService;
    private final AmapService amapService;

    @Operation(summary = "支付宝支付回调（含验签）")
    @PostMapping("/payment/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v != null && v.length > 0 ? v[0] : ""));
        log.info("[支付宝回调] 收到通知: paymentNo={}", params.get("out_trade_no"));
        return paymentService.handleAlipayNotify(params);
    }

    @Operation(summary = "支付宝支付回调（旧接口兼容）")
    @PostMapping("/payment/alipay/callback")
    public String alipayCallback(HttpServletRequest request) {
        return alipayNotify(request);
    }

    @Operation(summary = "微信支付回调")
    @PostMapping("/payment/wechat/callback")
    public Map<String, Object> wechatCallback(@RequestBody String body) {
        log.info("[微信回调] body={}", body);
        Map<String, Object> result = new HashMap<>();
        result.put("code", "SUCCESS");
        result.put("message", "成功");
        return result;
    }

    @Operation(summary = "逆地理编码")
    @GetMapping("/geo/reverse")
    public Result<RegeoResult> reverseGeocode(@RequestParam Double lng, @RequestParam Double lat) {
        return Result.ok(amapService.reverseGeocode(lng, lat));
    }

    @Operation(summary = "距离计算")
    @GetMapping("/geo/distance")
    public Result<Map<String, Object>> calculateDistance(@RequestParam Double lng1, @RequestParam Double lat1,
                                                         @RequestParam Double lng2, @RequestParam Double lat2) {
        long distance = amapService.calculateDistance(lng1, lat1, lng2, lat2);
        Map<String, Object> data = new HashMap<>();
        data.put("distance", distance);
        data.put("distanceKm", distance / 1000.0);
        data.put("withinRadius", amapService.isWithinDeliveryRadius(lng1, lat1, lng2, lat2));
        data.put("maxRadiusMeters", amapService.getMaxRadiusMeters());
        return Result.ok(data);
    }

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("ok");
    }
}
