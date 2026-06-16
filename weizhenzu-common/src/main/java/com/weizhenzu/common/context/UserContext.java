package com.weizhenzu.common.context;

import com.weizhenzu.common.enums.UserTypeEnum;
import lombok.Data;

/**
 * 用户上下文（ThreadLocal）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser u = get();
        return u == null ? null : u.getId();
    }

    public static UserTypeEnum getUserType() {
        LoginUser u = get();
        return u == null ? null : u.getUserType();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 登录用户信息
     */
    @Data
    public static class LoginUser {
        private Long id;
        private UserTypeEnum userType;
        private String username;
        private String nickname;
    }
}
