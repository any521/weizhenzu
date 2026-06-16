package com.weizhenzu.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_payment")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String paymentNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    private Integer payType;

    private Integer status;

    private String thirdPartyNo;

    private String payUrl;

    private LocalDateTime paidTime;

    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
