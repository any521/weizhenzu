package com.weizhenzu.application.service.impl;

import com.weizhenzu.application.service.AuthService;
import com.weizhenzu.application.service.SmsService;
import com.weizhenzu.common.constant.CommonConstants;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.enums.UserTypeEnum;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.common.utils.PhoneUtils;
import com.weizhenzu.domain.dto.PasswordLoginDTO;
import com.weizhenzu.domain.dto.RefreshTokenDTO;
import com.weizhenzu.domain.dto.SmsCodeDTO;
import com.weizhenzu.domain.dto.SmsLoginDTO;
import com.weizhenzu.domain.entity.Admin;
import com.weizhenzu.domain.entity.DeliveryMan;
import com.weizhenzu.domain.entity.Merchant;
import com.weizhenzu.domain.entity.User;
import com.weizhenzu.domain.vo.LoginVO;
import com.weizhenzu.infrastructure.persistence.mapper.AdminMapper;
import com.weizhenzu.infrastructure.persistence.mapper.DeliveryManMapper;
import com.weizhenzu.infrastructure.persistence.mapper.MerchantMapper;
import com.weizhenzu.infrastructure.persistence.mapper.UserMapper;
import com.weizhenzu.infrastructure.security.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;

/**
 * 认证服务实现
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SmsService smsService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final DeliveryManMapper deliveryManMapper;
    private final AdminMapper adminMapper;

    @Override
    public void sendSmsCode(SmsCodeDTO dto) {
        smsService.sendCode(dto.getPhone(), dto.getScene());
    }

    @Override
    public LoginVO smsLogin(SmsLoginDTO dto) {
        // 1. 校验验证码
        if (!smsService.verifyCode(dto.getPhone(), dto.getCode(), "LOGIN")) {
            throw new BizException(ResultCode.SMS_CODE_ERROR);
        }

        Integer userType = dto.getUserType() == null ? 1 : dto.getUserType();
        UserTypeEnum type = UserTypeEnum.of(userType);
        String phoneHash = PhoneUtils.hash(dto.getPhone());

        Long userId;
        String nickname = null;
        String avatar = null;

        switch (type) {
            case USER -> {
                User user = userMapper.selectByPhoneHash(phoneHash);
                if (user == null) {
                    // 自动注册
                    user = new User();
                    user.setPhone(PhoneUtils.encrypt(dto.getPhone()));
                    user.setPhoneHash(phoneHash);
                    user.setNickname("用户" + dto.getPhone().substring(7));
                    user.setStatus(1);
                    user.setLevel(1);
                    user.setPoints(0);
                    user.setBalance(java.math.BigDecimal.ZERO);
                    userMapper.insert(user);
                }
                checkUserStatus(user.getStatus(), user.getId());
                userId = user.getId();
                nickname = user.getNickname();
                avatar = user.getAvatar();
            }
            case MERCHANT -> {
                Merchant m = merchantMapper.selectByPhoneHash(phoneHash);
                if (m == null) throw new BizException(ResultCode.MERCHANT_NOT_FOUND);
                if (m.getStatus() != 1) throw new BizException(ResultCode.MERCHANT_AUDITING);
                userId = m.getId();
                nickname = m.getName();
                avatar = m.getLogo();
            }
            case RIDER -> {
                DeliveryMan r = deliveryManMapper.selectByPhoneHash(phoneHash);
                if (r == null) throw new BizException(ResultCode.USER_NOT_FOUND);
                if (r.getStatus() != 1) throw new BizException(ResultCode.USER_DISABLED);
                userId = r.getId();
                nickname = r.getName();
                avatar = r.getAvatar();
            }
            default -> throw new BizException(ResultCode.PARAM_ERROR, "该端不支持短信登录");
        }

        return buildLoginVO(userId, userType, nickname, avatar);
    }

    @Override
    public LoginVO passwordLogin(PasswordLoginDTO dto) {
        Integer userType = dto.getUserType() == null ? 1 : dto.getUserType();
        UserTypeEnum type = UserTypeEnum.of(userType);
        String phoneHash = PhoneUtils.hash(dto.getPhone());

        Long userId;
        String nickname = null;
        String avatar = null;

        switch (type) {
            case USER -> {
                User user = userMapper.selectByPhoneHash(phoneHash);
                if (user == null || user.getPassword() == null
                        || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                    throw new BizException(ResultCode.USER_NOT_FOUND, "账号或密码错误");
                }
                checkUserStatus(user.getStatus(), user.getId());
                userId = user.getId();
                nickname = user.getNickname();
                avatar = user.getAvatar();
            }
            case MERCHANT -> {
                Merchant m = merchantMapper.selectByPhoneHash(phoneHash);
                if (m == null || !passwordEncoder.matches(dto.getPassword(), m.getPassword())) {
                    throw new BizException(ResultCode.MERCHANT_NOT_FOUND, "账号或密码错误");
                }
                if (m.getStatus() != 1) throw new BizException(ResultCode.MERCHANT_AUDITING);
                userId = m.getId();
                nickname = m.getName();
                avatar = m.getLogo();
            }
            case RIDER -> {
                DeliveryMan r = deliveryManMapper.selectByPhoneHash(phoneHash);
                if (r == null || !passwordEncoder.matches(dto.getPassword(), r.getPassword())) {
                    throw new BizException(ResultCode.USER_NOT_FOUND, "账号或密码错误");
                }
                if (r.getStatus() != 1) throw new BizException(ResultCode.USER_DISABLED);
                userId = r.getId();
                nickname = r.getName();
                avatar = r.getAvatar();
            }
            case ADMIN -> {
                Admin admin = adminMapper.selectByUsername(dto.getPhone());
                if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
                    throw new BizException(ResultCode.USER_NOT_FOUND, "账号或密码错误");
                }
                if (admin.getStatus() != 1) throw new BizException(ResultCode.USER_DISABLED);
                userId = admin.getId();
                nickname = admin.getRealName();
                avatar = admin.getAvatar();
            }
            default -> throw new BizException(ResultCode.PARAM_ERROR, "不支持的用户类型");
        }

        return buildLoginVO(userId, userType, nickname, avatar);
    }

    @Override
    public LoginVO refresh(RefreshTokenDTO dto) {
        Claims claims = jwtUtils.parse(dto.getRefreshToken());
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BizException(ResultCode.UNAUTHORIZED, "非刷新令牌");
        }
        Long userId = Long.valueOf(claims.getSubject());
        Integer userType = claims.get("userType", Integer.class);
        return buildLoginVO(userId, userType, null, null);
    }

    @Override
    public void logout() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            redis.delete(CommonConstants.TOKEN_KEY + userId);
        }
    }

    private void checkUserStatus(Integer status, Long userId) {
        if (status == null || status == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
    }

    private LoginVO buildLoginVO(Long userId, Integer userType, String nickname, String avatar) {
        String token = jwtUtils.generate(userId, userType, new HashMap<>());
        String refreshToken = jwtUtils.generateRefresh(userId, userType);

        // 缓存 token
        redis.opsForValue().set(CommonConstants.TOKEN_KEY + userId, token,
                Duration.ofSeconds(jwtUtils.getExpireSeconds()));

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setRefreshToken(refreshToken);
        vo.setUserId(userId);
        vo.setNickname(nickname);
        vo.setAvatar(avatar);
        vo.setUserType(userType);
        return vo;
    }
}
