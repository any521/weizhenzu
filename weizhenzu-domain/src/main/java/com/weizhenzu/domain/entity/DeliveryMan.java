package com.weizhenzu.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 骑手
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_delivery_man")
public class DeliveryMan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String phone;

    private String phoneHash;

    private String password;

    private String name;

    private String avatar;

    private String idCard;

    private String realName;

    private Integer gender;

    private Integer level;

    private Integer status;

    private Integer onDuty;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private LocalDateTime locationTime;

    private Integer totalOrders;

    private Integer monthOrders;

    private BigDecimal rating;

    private BigDecimal balance;

    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(value = "deleted", select = false)
    private Integer deleted;

    @Version
    private Integer version;
}
