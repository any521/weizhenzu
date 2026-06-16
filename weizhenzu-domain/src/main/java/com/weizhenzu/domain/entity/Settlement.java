package com.weizhenzu.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商家结算单
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_settlement")
public class Settlement implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String settleNo;

    private Long merchantId;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private Integer orderCount;

    private BigDecimal orderAmount;

    private BigDecimal platformFee;

    private BigDecimal settleAmount;

    private Integer status;

    private LocalDateTime settleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
