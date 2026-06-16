package com.weizhenzu.common.annotation;

import com.weizhenzu.common.enums.UserTypeEnum;

import java.lang.annotation.*;

/**
 * 登录校验注解
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireLogin {

    /**
     * 允许的用户类型，为空表示任意已登录用户
     */
    UserTypeEnum[] value() default {};

    /**
     * 是否必须登录
     */
    boolean required() default true;
}
