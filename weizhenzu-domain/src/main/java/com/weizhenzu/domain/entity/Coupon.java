package com.weizhenzu.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_coupon")
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private Integer type;

    private BigDecimal amount;

    private BigDecimal threshold;

    private BigDecimal discount;

    private BigDecimal maxDiscount;

    private Integer totalCount;

    private Integer receivedCount;

    private Integer usedCount;

    private Integer perLimit;

    private Integer validType;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    private Integer validDays;

    private Integer scope;

    private String scopeIds;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(value = "deleted", select = false)
    private Integer deleted;
}
