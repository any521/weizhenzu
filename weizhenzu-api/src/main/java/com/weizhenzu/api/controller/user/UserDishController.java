package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.DishService;
import com.weizhenzu.application.service.ReviewService;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.vo.DishVO;
import com.weizhenzu.domain.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端菜品 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "C端-菜品", description = "C端菜品相关接口")
@RestController
@RequestMapping("/api/user/dishes")
@RequiredArgsConstructor
public class UserDishController {

    private final DishService dishService;
    private final ReviewService reviewService;

    @Operation(summary = "菜品详情")
    @GetMapping("/{id}")
    public Result<DishVO> detail(@PathVariable Long id) {
        return Result.ok(dishService.detail(id));
    }

    @Operation(summary = "菜品评价列表")
    @GetMapping("/{id}/reviews")
    public Result<PageResult<ReviewVO>> reviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(reviewService.dishReviews(id, current, size));
    }
}
