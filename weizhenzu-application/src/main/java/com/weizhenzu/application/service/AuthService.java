package com.weizhenzu.application.service;

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
     * 发送邮箱验证码（真实 SMTP 发送）
     */
    void sendEmailCode(EmailCodeDTO dto);

    /**
     * 短信验证码登录
     */
    LoginVO smsLogin(SmsLoginDTO dto);

    /**
     * 邮箱验证码登录
     */
    LoginVO emailLogin(EmailLoginDTO dto);

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
     * C端用户注册（邮箱验证码）
     */
    Long registerUserByEmail(EmailRegisterDTO dto);

    /**
     * 骑手注册（短信验证码，注册后需管理员审核）
     */
    Long registerRider(RiderRegisterDTO dto);

    /**
     * 骑手注册（邮箱验证码，注册后需管理员审核）
     */
    Long registerRiderByEmail(RiderEmailRegisterDTO dto);

    /**
     * 绑定手机号（登录后调用，需邮箱验证码校验）
     */
    void bindPhone(BindPhoneDTO dto);
}
