package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.AddressService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.AddressDTO;
import com.weizhenzu.domain.vo.AddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端用户地址 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "C端-收货地址", description = "用户收货地址相关接口")
@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final AddressService addressService;

    @Operation(summary = "地址列表")
    @GetMapping
    public Result<List<AddressVO>> list() {
        return Result.ok(addressService.list());
    }

    @Operation(summary = "地址详情")
    @GetMapping("/{id}")
    public Result<AddressVO> detail(@PathVariable Long id) {
        return Result.ok(addressService.detail(id));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody AddressDTO dto) {
        return Result.ok(addressService.add(dto));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        addressService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "设为默认")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.ok();
    }
}
