package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.AuthService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.PasswordLoginDTO;
import com.weizhenzu.domain.dto.RefreshTokenDTO;
import com.weizhenzu.domain.dto.SmsCodeDTO;
import com.weizhenzu.domain.dto.SmsLoginDTO;
import com.weizhenzu.domain.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端用户认证 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "C端-认证", description = "用户认证相关接口")
@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final AuthService authService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsCodeDTO dto) {
        authService.sendSmsCode(dto);
        return Result.ok();
    }

    @Operation(summary = "短信验证码登录")
    @PostMapping("/login/sms")
    public Result<LoginVO> smsLogin(@Valid @RequestBody SmsLoginDTO dto) {
        dto.setUserType(1);
        return Result.ok(authService.smsLogin(dto));
    }

    @Operation(summary = "密码登录")
    @PostMapping("/login/password")
    public Result<LoginVO> passwordLogin(@Valid @RequestBody PasswordLoginDTO dto) {
        dto.setUserType(1);
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
