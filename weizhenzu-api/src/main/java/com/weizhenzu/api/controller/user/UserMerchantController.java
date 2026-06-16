package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.MerchantService;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.vo.DishCategoryVO;
import com.weizhenzu.domain.vo.MerchantCategoryVO;
import com.weizhenzu.domain.vo.MerchantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端商家 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "C端-商家", description = "C端商家相关接口")
@RestController
@RequestMapping("/api/user/merchants")
@RequiredArgsConstructor
public class UserMerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "商家列表")
    @GetMapping
    public Result<PageResult<MerchantVO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(merchantService.userPage(current, size, categoryId, keyword));
    }

    @Operation(summary = "商家分类列表")
    @GetMapping("/categories")
    public Result<List<MerchantCategoryVO>> categories() {
        return Result.ok(merchantService.categories());
    }

    @Operation(summary = "商家详情")
    @GetMapping("/{id}")
    public Result<MerchantVO> detail(@PathVariable Long id) {
        return Result.ok(merchantService.detail(id));
    }

    @Operation(summary = "商家菜单")
    @GetMapping("/{id}/menu")
    public Result<List<DishCategoryVO>> menu(@PathVariable Long id) {
        return Result.ok(merchantService.menu(id));
    }
}
