package com.weizhenzu.common.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * <p>基于 Redis + Lua 实现</p>
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

    /**
     * 限流键 SpEL 表达式
     */
    String key();

    /**
     * 时间窗口（秒）
     */
    int time() default 1;

    /**
     * 时间窗口内最大请求数
     */
    int count() default 10;

    String message() default "请求过于频繁";
}
