package com.weizhenzu.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * C端用户实体
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    private String username;
    private String phone;
    private String phoneHash;
    private String email;
    private String password;
    private String nickname;
    private String avatar;
    private Integer gender;
    private java.time.LocalDate birthday;
    private Integer status;
    private String realName;
    private String idCard;
    private Integer level;
    private Integer points;
    private BigDecimal balance;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
}
