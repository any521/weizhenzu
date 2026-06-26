package com.weizhenzu.application.service;

import com.weizhenzu.domain.dto.PasswordUpdateDTO;
import com.weizhenzu.domain.vo.UserVO;

import java.time.LocalDate;

/**
 * 用户服务接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 获取当前用户信息
     */
    UserVO currentUser();

    /**
     * 更新用户资料
     */
    void updateProfile(String nickname, String avatar, Integer gender, LocalDate birthday);

    /**
     * 修改密码
     */
    void updatePassword(PasswordUpdateDTO dto);

    /**
     * 解绑手机号
     */
    void unbindPhone();
}
