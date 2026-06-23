package com.weizhenzu.application.service;

import com.weizhenzu.domain.dto.PasswordLoginDTO;
import com.weizhenzu.domain.dto.RefreshTokenDTO;
import com.weizhenzu.domain.dto.RiderRegisterDTO;
import com.weizhenzu.domain.dto.SmsCodeDTO;
import com.weizhenzu.domain.dto.SmsLoginDTO;
import com.weizhenzu.domain.dto.UserRegisterDTO;
import com.weizhenzu.domain.vo.LoginVO;

/**
 * 认证服务接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * 发送短信验证码
     */
    void sendSmsCode(SmsCodeDTO dto);

    /**
     * 短信验证码登录
     */
    LoginVO smsLogin(SmsLoginDTO dto);

    /**
     * 密码登录
     */
    LoginVO passwordLogin(PasswordLoginDTO dto);

    /**
     * 刷新 Token
     */
    LoginVO refresh(RefreshTokenDTO dto);

    /**
     * 退出登录
     */
    void logout();

    /**
     * C端用户注册（短信验证码）
     */
    Long registerUser(UserRegisterDTO dto);

    /**
     * 骑手注册（短信验证码，注册后需管理员审核）
     */
    Long registerRider(RiderRegisterDTO dto);
}
