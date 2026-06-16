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
 * 退款单
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_refund")
public class Refund implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String refundNo;

    private Long orderId;

    private String orderNo;

    private Long paymentId;

    private Long userId;

    private Long merchantId;

    private BigDecimal amount;

    private String reason;

    private Integer status;

    private String thirdPartyNo;

    private String auditRemark;

    private Long auditorId;

    private LocalDateTime refundTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
