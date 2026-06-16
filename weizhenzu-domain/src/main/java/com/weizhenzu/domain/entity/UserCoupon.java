package com.weizhenzu.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户优惠券
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long couponId;

    private String couponName;

    private String couponSnapshot;

    private Integer status;

    private Long orderId;

    private LocalDateTime usedTime;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
