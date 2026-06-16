package com.weizhenzu.api.controller.merchant;

import com.weizhenzu.application.service.AuthService;
import com.weizhenzu.application.service.MerchantService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.MerchantRegisterDTO;
import com.weizhenzu.domain.dto.PasswordLoginDTO;
import com.weizhenzu.domain.dto.RefreshTokenDTO;
import com.weizhenzu.domain.vo.LoginVO;
import com.weizhenzu.domain.vo.MerchantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * B端商家认证 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "B端-认证", description = "商家认证相关接口")
@RestController
@RequestMapping("/api/merchant/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final AuthService authService;
    private final MerchantService merchantService;

    @Operation(summary = "商家注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody MerchantRegisterDTO dto) {
        return Result.ok(merchantService.register(dto));
    }

    @Operation(summary = "密码登录")
    @PostMapping("/login/password")
    public Result<LoginVO> passwordLogin(@Valid @RequestBody PasswordLoginDTO dto) {
        dto.setUserType(2);
        return Result.ok(authService.passwordLogin(dto));
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.ok(authService.refresh(dto));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "当前商家信息")
    @GetMapping("/me")
    public Result<MerchantVO> me() {
        return Result.ok(merchantService.current());
    }
}
