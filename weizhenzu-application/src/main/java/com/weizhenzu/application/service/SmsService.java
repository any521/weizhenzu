package com.weizhenzu.application.service;

/**
 * 短信服务接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @param scene 场景：LOGIN/REGISTER/RESET
     */
    void sendCode(String phone, String scene);

    /**
     * 校验验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @param scene 场景
     * @return 是否校验通过
     */
    boolean verifyCode(String phone, String code, String scene);
}
