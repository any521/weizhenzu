package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.CartService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.CartAddDTO;
import com.weizhenzu.domain.dto.CartUpdateDTO;
import com.weizhenzu.domain.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端购物车 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "C端-购物车", description = "购物车相关接口")
@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
public class UserCartController {

    private final CartService cartService;

    @Operation(summary = "获取购物车")
    @GetMapping
    public Result<CartVO> getCart() {
        return Result.ok(cartService.getCart());
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return Result.ok();
    }

    @Operation(summary = "修改购物车数量")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CartUpdateDTO dto) {
        cartService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear();
        return Result.ok();
    }
}
