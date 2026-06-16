package com.weizhenzu.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 幂等注解
 * <p>基于 Redis SETNX 实现接口幂等性</p>
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等键 SpEL 表达式
     */
    String key();

    /**
     * 过期时间，默认 10 秒
     */
    long expire() default 10;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 幂等失败提示信息
     */
    String message() default "请勿重复操作";
}
