package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.ReviewService;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.ReviewCreateDTO;
import com.weizhenzu.domain.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端评价 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "C端-评价", description = "用户评价相关接口")
@RestController
@RequestMapping("/api/user/reviews")
@RequiredArgsConstructor
public class UserReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "我的评价列表")
    @GetMapping
    public Result<PageResult<ReviewVO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(reviewService.userPage(current, size));
    }

    @Operation(summary = "创建评价")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ReviewCreateDTO dto) {
        return Result.ok(reviewService.create(dto));
    }

    @Operation(summary = "评价详情")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(reviewService.detail(id));
    }
}
