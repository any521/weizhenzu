package com.weizhenzu.api.controller.rider;

import com.weizhenzu.application.service.AuthService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.BindPhoneDTO;
import com.weizhenzu.domain.dto.EmailCodeDTO;
import com.weizhenzu.domain.dto.EmailLoginDTO;
import com.weizhenzu.domain.dto.PasswordLoginDTO;
import com.weizhenzu.domain.dto.RefreshTokenDTO;
import com.weizhenzu.domain.dto.RiderEmailRegisterDTO;
import com.weizhenzu.domain.dto.RiderRegisterDTO;
import com.weizhenzu.domain.dto.SmsCodeDTO;
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

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsCodeDTO dto) {
        authService.sendSmsCode(dto);
        return Result.ok();
    }

    @Operation(summary = "发送邮箱验证码（真实 SMTP 发送）")
    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody EmailCodeDTO dto) {
        dto.setUserType(3);
        authService.sendEmailCode(dto);
        return Result.ok();
    }

    @Operation(summary = "骑手注册（短信验证码）")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RiderRegisterDTO dto) {
        return Result.ok(authService.registerRider(dto));
    }

    @Operation(summary = "骑手注册（邮箱验证码）")
    @PostMapping("/register/email")
    public Result<Long> registerByEmail(@Valid @RequestBody RiderEmailRegisterDTO dto) {
        return Result.ok(authService.registerRiderByEmail(dto));
    }

    @Operation(summary = "短信验证码登录")
    @PostMapping("/login/sms")
    public Result<LoginVO> smsLogin(@Valid @RequestBody SmsLoginDTO dto) {
        dto.setUserType(3);
        return Result.ok(authService.smsLogin(dto));
    }

    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/login/email")
    public Result<LoginVO> emailLogin(@Valid @RequestBody EmailLoginDTO dto) {
        dto.setUserType(3);
        return Result.ok(authService.emailLogin(dto));
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

    @Operation(summary = "绑定手机号（接单前必须绑定）")
    @PostMapping("/bind-phone")
    public Result<Void> bindPhone(@Valid @RequestBody BindPhoneDTO dto) {
        authService.bindPhone(dto);
        return Result.ok();
    }
}
