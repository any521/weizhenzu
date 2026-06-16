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
 * 订单明细
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long dishId;

    private String dishName;

    private String dishImage;

    private Long specId;

    private String specName;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal subtotal;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
