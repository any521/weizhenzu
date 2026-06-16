package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.DishService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.vo.DishVO;
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

    @Operation(summary = "菜品详情")
    @GetMapping("/{id}")
    public Result<DishVO> detail(@PathVariable Long id) {
        return Result.ok(dishService.detail(id));
    }
}
