package com.weizhenzu.api.controller.merchant;

import com.weizhenzu.application.service.ReviewService;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * B端评价 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "B端-评价", description = "商家评价管理接口")
@RestController
@RequestMapping("/api/merchant/reviews")
@RequiredArgsConstructor
public class MerchantReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "评价列表")
    @GetMapping
    public Result<PageResult<ReviewVO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer rating) {
        return Result.ok(reviewService.merchantPage(current, size, rating));
    }

    @Operation(summary = "回复评价")
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reviewService.reply(id, body.get("content"));
        return Result.ok();
    }

    @Operation(summary = "更新评价状态（隐藏/公开）")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") == null ? null : Integer.valueOf(body.get("status").toString());
        reviewService.updateStatus(id, status);
        return Result.ok();
    }
}
