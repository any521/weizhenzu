package com.weizhenzu.application.service.impl;

import com.weizhenzu.application.service.AuthService;
import com.weizhenzu.application.service.EmailService;
import com.weizhenzu.application.service.SmsService;
import com.weizhenzu.common.constant.CommonConstants;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.enums.UserTypeEnum;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.common.utils.PhoneUtils;
import com.weizhenzu.domain.dto.BindPhoneDTO;
import com.weizhenzu.domain.dto.EmailCodeDTO;
import com.weizhenzu.domain.dto.EmailLoginDTO;
import com.weizhenzu.domain.dto.EmailRegisterDTO;
import com.weizhenzu.domain.dto.PasswordLoginDTO;
import com.weizhenzu.domain.dto.RefreshTokenDTO;
import com.weizhenzu.domain.dto.RiderEmailRegisterDTO;
import com.weizhenzu.domain.dto.RiderRegisterDTO;
import com.weizhenzu.domain.dto.SmsCodeDTO;
import com.weizhenzu.domain.dto.SmsLoginDTO;
import com.weizhenzu.domain.dto.UserRegisterDTO;
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
    private final EmailService emailService;
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
    public void sendEmailCode(EmailCodeDTO dto) {
        String email = dto.getEmail();
        String scene = dto.getScene();
        // 绑定手机号场景：从当前登录用户上下文获取真实邮箱，避免使用前端传来的脱敏邮箱
        if ("BIND_PHONE".equals(scene)) {
            Long userId = UserContext.getUserId();
            UserTypeEnum userTypeEnum = UserContext.getUserType();
            if (userId == null || userTypeEnum == null) {
                throw new BizException(ResultCode.UNAUTHORIZED);
            }
            email = getCurrentEmail(userId, userTypeEnum.getCode());
            if (email == null || email.isBlank()) {
                throw new BizException(ResultCode.PARAM_ERROR, "当前账号未绑定邮箱，无法发送验证码");
            }
        }
        emailService.sendCode(email, scene);
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
                Merchant m = merchantMapper.selectByPhoneHashRaw(phoneHash);
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
            case ADMIN -> {
                Admin admin = adminMapper.selectByPhoneHash(phoneHash);
                if (admin == null) throw new BizException(ResultCode.USER_NOT_FOUND, "手机号未绑定管理员");
                if (admin.getStatus() != 1) throw new BizException(ResultCode.USER_DISABLED);
                userId = admin.getId();
                nickname = admin.getRealName();
                avatar = admin.getAvatar();
            }
            default -> throw new BizException(ResultCode.PARAM_ERROR, "该端不支持短信登录");
        }

        return buildLoginVO(userId, userType, nickname, avatar);
    }

    @Override
    public LoginVO emailLogin(EmailLoginDTO dto) {
        // 1. 校验邮箱验证码
        if (!emailService.verifyCode(dto.getEmail(), dto.getCode(), "LOGIN")) {
            throw new BizException(ResultCode.SMS_CODE_ERROR, "验证码错误或已过期");
        }

        Integer userType = dto.getUserType() == null ? 1 : dto.getUserType();
        UserTypeEnum type = UserTypeEnum.of(userType);

        Long userId;
        String nickname = null;
        String avatar = null;

        switch (type) {
            case USER -> {
                User user = userMapper.selectByEmail(dto.getEmail());
                if (user == null) {
                    // 自动注册
                    user = new User();
                    user.setEmail(dto.getEmail());
                    user.setNickname("用户" + dto.getEmail().substring(0, Math.min(6, dto.getEmail().length())));
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
            case RIDER -> {
                DeliveryMan r = deliveryManMapper.selectByEmail(dto.getEmail());
                if (r == null) throw new BizException(ResultCode.EMAIL_NOT_FOUND, "该邮箱尚未注册骑手账号");
                if (r.getStatus() == 0) throw new BizException(ResultCode.ACCOUNT_AUDITING);
                if (r.getStatus() != 1) throw new BizException(ResultCode.USER_DISABLED);
                userId = r.getId();
                nickname = r.getName();
                avatar = r.getAvatar();
            }
            default -> throw new BizException(ResultCode.PARAM_ERROR, "该端不支持邮箱登录");
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
                // 未命中手机号且输入的是用户名，则按用户名查询
                if (user == null && !PhoneUtils.isPhone(dto.getPhone())) {
                    user = userMapper.selectByUsername(dto.getPhone());
                }
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
                Merchant m = merchantMapper.selectByPhoneHashRaw(phoneHash);
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
                if (r == null || r.getPassword() == null
                        || !passwordEncoder.matches(dto.getPassword(), r.getPassword())) {
                    throw new BizException(ResultCode.USER_NOT_FOUND, "账号或密码错误");
                }
                if (r.getStatus() != 1) throw new BizException(ResultCode.USER_DISABLED);
                userId = r.getId();
                nickname = r.getName();
                avatar = r.getAvatar();
            }
            case ADMIN -> {
                // 管理员支持用户名+密码 或 手机号+密码
                Admin admin = adminMapper.selectByUsername(dto.getPhone());
                if (admin == null && PhoneUtils.isPhone(dto.getPhone())) {
                    admin = adminMapper.selectByPhoneHash(PhoneUtils.hash(dto.getPhone()));
                }
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

    @Override
    public Long registerUser(UserRegisterDTO dto) {
        // 1. 校验短信验证码
        if (!smsService.verifyCode(dto.getPhone(), dto.getCode(), "REGISTER")) {
            throw new BizException(ResultCode.SMS_CODE_ERROR);
        }
        // 2. 校验手机号是否已注册
        String phoneHash = PhoneUtils.hash(dto.getPhone());
        if (userMapper.selectByPhoneHash(phoneHash) != null) {
            throw new BizException(ResultCode.PARAM_ERROR, "该手机号已注册");
        }
        // 3. 校验用户名唯一性
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            if (userMapper.selectByUsername(dto.getUsername()) != null) {
                throw new BizException(ResultCode.PARAM_ERROR, "该用户名已被占用");
            }
        }
        // 4. 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPhone(PhoneUtils.encrypt(dto.getPhone()));
        user.setPhoneHash(phoneHash);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null && !dto.getNickname().isBlank()
                ? dto.getNickname() : "用户" + dto.getPhone().substring(7));
        user.setStatus(1);
        user.setLevel(1);
        user.setPoints(0);
        user.setBalance(java.math.BigDecimal.ZERO);
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public Long registerRider(RiderRegisterDTO dto) {
        // 1. 校验短信验证码
        if (!smsService.verifyCode(dto.getPhone(), dto.getCode(), "REGISTER")) {
            throw new BizException(ResultCode.SMS_CODE_ERROR);
        }
        // 2. 校验手机号是否已注册
        String phoneHash = PhoneUtils.hash(dto.getPhone());
        if (deliveryManMapper.selectByPhoneHash(phoneHash) != null) {
            throw new BizException(ResultCode.PARAM_ERROR, "该手机号已注册");
        }
        // 3. 创建骑手（status=0 待审核）
        DeliveryMan rider = new DeliveryMan();
        rider.setPhone(PhoneUtils.encrypt(dto.getPhone()));
        rider.setPhoneHash(phoneHash);
        rider.setPassword(passwordEncoder.encode(dto.getPassword()));
        rider.setName(dto.getRealName());
        rider.setRealName(dto.getRealName());
        rider.setIdCard(dto.getIdCard());
        rider.setStatus(0); // 0=待审核，需管理员审核通过后才能登录
        rider.setOnDuty(0);
        rider.setTotalOrders(0);
        rider.setMonthOrders(0);
        rider.setRating(new java.math.BigDecimal("5.0"));
        rider.setBalance(java.math.BigDecimal.ZERO);
        deliveryManMapper.insert(rider);
        return rider.getId();
    }

    @Override
    public Long registerUserByEmail(EmailRegisterDTO dto) {
        // 1. 校验邮箱验证码
        if (!emailService.verifyCode(dto.getEmail(), dto.getCode(), "REGISTER")) {
            throw new BizException(ResultCode.SMS_CODE_ERROR, "验证码错误或已过期");
        }
        // 2. 校验邮箱是否已注册
        if (userMapper.selectByEmail(dto.getEmail()) != null) {
            throw new BizException(ResultCode.EMAIL_EXISTS);
        }
        // 3. 校验用户名唯一性
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            if (userMapper.selectByUsername(dto.getUsername()) != null) {
                throw new BizException(ResultCode.PARAM_ERROR, "该用户名已被占用");
            }
        }
        // 4. 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null && !dto.getNickname().isBlank()
                ? dto.getNickname() : "用户" + dto.getEmail().substring(0, Math.min(6, dto.getEmail().length())));
        user.setStatus(1);
        user.setLevel(1);
        user.setPoints(0);
        user.setBalance(java.math.BigDecimal.ZERO);
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public Long registerRiderByEmail(RiderEmailRegisterDTO dto) {
        // 1. 校验邮箱验证码
        if (!emailService.verifyCode(dto.getEmail(), dto.getCode(), "REGISTER")) {
            throw new BizException(ResultCode.SMS_CODE_ERROR, "验证码错误或已过期");
        }
        // 2. 校验邮箱是否已注册
        if (deliveryManMapper.selectByEmail(dto.getEmail()) != null) {
            throw new BizException(ResultCode.EMAIL_EXISTS);
        }
        // 3. 创建骑手（status=0 待审核）
        DeliveryMan rider = new DeliveryMan();
        rider.setEmail(dto.getEmail());
        rider.setName(dto.getName());
        rider.setRealName(dto.getName());
        rider.setVehicleNo(dto.getVehicleNo());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            rider.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        rider.setStatus(0); // 0=待审核，需管理员审核通过后才能登录
        rider.setOnDuty(0);
        rider.setTotalOrders(0);
        rider.setMonthOrders(0);
        rider.setRating(new java.math.BigDecimal("5.0"));
        rider.setBalance(java.math.BigDecimal.ZERO);
        deliveryManMapper.insert(rider);
        return rider.getId();
    }

    @Override
    public void bindPhone(BindPhoneDTO dto) {
        Long userId = UserContext.getUserId();
        UserTypeEnum userTypeEnum = UserContext.getUserType();
        if (userId == null || userTypeEnum == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        int userType = userTypeEnum.getCode();

        // 1. 校验手机号格式
        if (!PhoneUtils.isPhone(dto.getPhone())) {
            throw new BizException(ResultCode.PHONE_FORMAT_ERROR);
        }

        // 2. 校验手机号是否已被其他账号绑定
        String phoneHash = PhoneUtils.hash(dto.getPhone());
        if (userType == UserTypeEnum.USER.getCode()) {
            User existing = userMapper.selectByPhoneHash(phoneHash);
            if (existing != null && !existing.getId().equals(userId)) {
                throw new BizException(ResultCode.PHONE_ALREADY_BOUND);
            }
        } else if (userType == UserTypeEnum.RIDER.getCode()) {
            DeliveryMan existing = deliveryManMapper.selectByPhoneHash(phoneHash);
            if (existing != null && !existing.getId().equals(userId)) {
                throw new BizException(ResultCode.PHONE_ALREADY_BOUND);
            }
        }

        // 3. 校验邮箱验证码（发送到当前账号邮箱，确保是本人操作）
        String email = getCurrentEmail(userId, userType);
        if (email == null || email.isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "当前账号未绑定邮箱，无法绑定手机号");
        }
        if (!emailService.verifyCode(email, dto.getCode(), "BIND_PHONE")) {
            throw new BizException(ResultCode.SMS_CODE_ERROR, "验证码错误或已过期");
        }

        // 4. 绑定手机号
        if (userType == UserTypeEnum.USER.getCode()) {
            User update = new User();
            update.setId(userId);
            update.setPhone(PhoneUtils.encrypt(dto.getPhone()));
            update.setPhoneHash(phoneHash);
            userMapper.updateById(update);
        } else if (userType == UserTypeEnum.RIDER.getCode()) {
            DeliveryMan update = new DeliveryMan();
            update.setId(userId);
            update.setPhone(PhoneUtils.encrypt(dto.getPhone()));
            update.setPhoneHash(phoneHash);
            deliveryManMapper.updateById(update);
        }
        log.info("[绑定手机号] userId={}, userType={}, phone={}", userId, userType, PhoneUtils.mask(dto.getPhone()));
    }

    /**
     * 获取当前登录账号的邮箱
     */
    private String getCurrentEmail(Long userId, Integer userType) {
        if (userType == UserTypeEnum.USER.getCode()) {
            User user = userMapper.selectById(userId);
            return user != null ? user.getEmail() : null;
        } else if (userType == UserTypeEnum.RIDER.getCode()) {
            DeliveryMan rider = deliveryManMapper.selectById(userId);
            return rider != null ? rider.getEmail() : null;
        }
        return null;
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
