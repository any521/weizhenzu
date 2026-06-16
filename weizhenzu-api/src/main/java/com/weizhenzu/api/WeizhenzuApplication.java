package com.weizhenzu.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 味真足外卖订餐系统启动类
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.weizhenzu")
@MapperScan("com.weizhenzu.infrastructure.persistence.mapper")
@EnableAsync
@EnableScheduling
public class WeizhenzuApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeizhenzuApplication.class, args);
    }
}
