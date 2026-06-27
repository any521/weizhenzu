package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.DishService;
import com.weizhenzu.application.service.ReviewService;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.vo.DishVO;
import com.weizhenzu.domain.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        DishVO vo = dishService.detail(id);
        // 填充商家名称（详情页需要）
        if (vo != null && vo.getMerchantId() != null) {
            // detail中已填充了分类，但可能未填商家名称，这里让service处理
        }
        return Result.ok(vo);
    }

    @Operation(summary = "菜品评价列表")
    @GetMapping("/{id}/reviews")
    public Result<PageResult<ReviewVO>> reviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(reviewService.dishReviews(id, current, size));
    }

    @Operation(summary = "C端菜品列表（搜索/分类）")
    @GetMapping
    public Result<PageResult<DishVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "平台分类ID") @RequestParam(required = false) Long platformCategoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return Result.ok(dishService.userPage(current, size, platformCategoryId, keyword));
    }

    @Operation(summary = "精选菜品（最近上架）")
    @GetMapping("/featured")
    public Result<List<DishVO>> featured(
            @Parameter(description = "数量，默认5") @RequestParam(defaultValue = "5") Integer limit,
            @Parameter(description = "用餐类型: 2=外卖, 3=自取") @RequestParam(required = false) Integer diningType) {
        return Result.ok(dishService.featuredDishes(limit, diningType));
    }
}
