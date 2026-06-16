package com.weizhenzu.api.controller.rider;

import com.weizhenzu.application.service.AuthService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.PasswordLoginDTO;
import com.weizhenzu.domain.dto.RefreshTokenDTO;
import com.weizhenzu.domain.dto.SmsLoginDTO;
import com.weizhenzu.domain.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 骑手端认证 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "骑手端-认证", description = "骑手认证相关接口")
@RestController
@RequestMapping("/api/rider/auth")
@RequiredArgsConstructor
public class RiderAuthController {

    private final AuthService authService;

    @Operation(summary = "短信验证码登录")
    @PostMapping("/login/sms")
    public Result<LoginVO> smsLogin(@Valid @RequestBody SmsLoginDTO dto) {
        dto.setUserType(3);
        return Result.ok(authService.smsLogin(dto));
    }

    @Operation(summary = "密码登录")
    @PostMapping("/login/password")
    public Result<LoginVO> passwordLogin(@Valid @RequestBody PasswordLoginDTO dto) {
        dto.setUserType(3);
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
}
