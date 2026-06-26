package com.weizhenzu.application.service.impl;

import com.weizhenzu.application.service.UserService;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.common.utils.PhoneUtils;
import com.weizhenzu.domain.dto.PasswordUpdateDTO;
import com.weizhenzu.domain.entity.User;
import com.weizhenzu.domain.vo.UserVO;
import com.weizhenzu.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 用户服务实现
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserVO currentUser() {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        // 手机号脱敏
        String decryptedPhone = PhoneUtils.decrypt(user.getPhone());
        vo.setPhone(PhoneUtils.mask(decryptedPhone));
        vo.setPhoneBound(decryptedPhone != null && !decryptedPhone.isBlank());
        // 邮箱脱敏
        vo.setEmail(maskEmail(user.getEmail()));
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setBirthday(user.getBirthday());
        vo.setLevel(user.getLevel());
        vo.setPoints(user.getPoints());
        vo.setBalance(user.getBalance());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    /**
     * 邮箱脱敏：u***@example.com
     */
    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }

    @Override
    public void updateProfile(String nickname, String avatar, Integer gender, LocalDate birthday) {
        Long userId = UserContext.getUserId();
        User user = new User();
        user.setId(userId);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setGender(gender);
        user.setBirthday(birthday);
        userMapper.updateById(user);
    }

    @Override
    public void updatePassword(PasswordUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getPassword() != null
                && !passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PARAM_ERROR, "原密码错误");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    @Override
    public void unbindPhone() {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        String decryptedPhone = PhoneUtils.decrypt(user.getPhone());
        if (decryptedPhone == null || decryptedPhone.isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "当前账号未绑定手机号");
        }
        User update = new User();
        update.setId(userId);
        update.setPhone(null);
        update.setPhoneHash(null);
        userMapper.updateById(update);
    }
}
