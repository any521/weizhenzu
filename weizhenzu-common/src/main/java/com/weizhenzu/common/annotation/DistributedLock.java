package com.weizhenzu.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * <p>基于 Redisson 实现</p>
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁键 SpEL 表达式
     */
    String key();

    /**
     * 等待时间（秒）
     */
    long waitTime() default 3;

    /**
     * 持有时间（秒）
     */
    long leaseTime() default 30;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    String message() default "操作过于频繁，请稍后再试";
}
