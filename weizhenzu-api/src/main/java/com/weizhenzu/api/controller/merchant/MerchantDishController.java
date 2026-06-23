package com.weizhenzu.api.controller.merchant;

import com.weizhenzu.application.service.DishService;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.DishDTO;
import com.weizhenzu.domain.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * B端菜品 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "B端-菜品", description = "商家菜品管理接口")
@RestController
@RequestMapping("/api/merchant/dishes")
@RequiredArgsConstructor
public class MerchantDishController {

    private final DishService dishService;

    @Operation(summary = "菜品分页")
    @GetMapping
    public Result<PageResult<DishVO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(dishService.merchantPage(current, size, categoryId, keyword));
    }

    @Operation(summary = "菜品详情")
    @GetMapping("/{id}")
    public Result<DishVO> detail(@PathVariable Long id) {
        return Result.ok(dishService.merchantDetail(id));
    }

    @Operation(summary = "新增菜品")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody DishDTO dto) {
        return Result.ok(dishService.add(dto));
    }

    @Operation(summary = "修改菜品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DishDTO dto) {
        dishService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "上下架菜品")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        dishService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除菜品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dishService.delete(id);
        return Result.ok();
    }
}
