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
 * 配送任务
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_delivery_task")
public class DeliveryTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;

    private Long orderId;

    private String orderNo;

    private Long deliveryManId;

    private Long merchantId;

    private String merchantName;

    private String merchantAddress;

    private BigDecimal merchantLng;

    private BigDecimal merchantLat;

    private String userAddress;

    private BigDecimal userLng;

    private BigDecimal userLat;

    private String userPhone;

    private Integer status;

    private BigDecimal fee;

    private Integer distance;

    private LocalDateTime grabTime;

    private LocalDateTime arriveTime;

    private LocalDateTime pickupTime;

    private LocalDateTime deliverTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
