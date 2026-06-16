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
 * 订单状态流转日志
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_order_log")
public class OrderLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Integer fromStatus;

    private Integer toStatus;

    private Integer operatorType;

    private Long operatorId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
