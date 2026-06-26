package com.weizhenzu.api.controller.admin;

import com.weizhenzu.application.service.AdminService;
import com.weizhenzu.application.service.AuthService;
import com.weizhenzu.common.annotation.RequireLogin;
import com.weizhenzu.common.enums.UserTypeEnum;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.AdminPasswordLoginDTO;
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
 * 管理后台认证 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "管理后台-认证", description = "管理员认证相关接口")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final AdminService adminService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsCodeDTO dto) {
        authService.sendSmsCode(dto);
        return Result.ok();
    }

    @Operation(summary = "密码登录")
    @PostMapping("/login/password")
    public Result<LoginVO> passwordLogin(@Valid @RequestBody AdminPasswordLoginDTO dto) {
        PasswordLoginDTO loginDto = new PasswordLoginDTO();
        loginDto.setPhone(dto.getUsername());
        loginDto.setPassword(dto.getPassword());
        loginDto.setUserType(4);
        return Result.ok(authService.passwordLogin(loginDto));
    }

    @Operation(summary = "短信验证码登录")
    @PostMapping("/login/sms")
    public Result<LoginVO> smsLogin(@Valid @RequestBody SmsLoginDTO dto) {
        dto.setUserType(4);
        return Result.ok(authService.smsLogin(dto));
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

    @Operation(summary = "获取当前管理员信息")
    @GetMapping("/info")
    @RequireLogin(UserTypeEnum.ADMIN)
    public Result<java.util.Map<String, Object>> info() {
        return Result.ok(adminService.getCurrentProfile());
    }
}
