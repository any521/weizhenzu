package com.weizhenzu.api.controller.user;

import com.weizhenzu.application.service.UserService;
import com.weizhenzu.common.result.Result;
import com.weizhenzu.domain.dto.PasswordUpdateDTO;
import com.weizhenzu.domain.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * C端用户资料 Controller
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Tag(name = "C端-用户资料", description = "用户资料相关接口")
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @Operation(summary = "当前用户信息")
    @GetMapping
    public Result<UserVO> currentUser() {
        return Result.ok(userService.currentUser());
    }

    @Operation(summary = "更新资料")
    @PutMapping
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        String nickname = (String) body.get("nickname");
        String avatar = (String) body.get("avatar");
        Integer gender = body.get("gender") == null ? null : Integer.valueOf(body.get("gender").toString());
        String birthdayStr = (String) body.get("birthday");
        LocalDate birthday = birthdayStr == null ? null : LocalDate.parse(birthdayStr);
        userService.updateProfile(nickname, avatar, gender, birthday);
        return Result.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        userService.updatePassword(dto);
        return Result.ok();
    }
}
