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
 * 订单主表
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_order")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long merchantId;

    private Long addressId;

    private String addressSnapshot;

    private Integer status;

    private Integer payStatus;

    private Integer itemCount;

    private BigDecimal totalAmount;

    private BigDecimal packingFee;

    private BigDecimal deliveryFee;

    private BigDecimal merchantDiscount;

    private BigDecimal platformDiscount;

    private BigDecimal couponAmount;

    private BigDecimal payAmount;

    private Long userCouponId;

    private String remark;

    private Integer payType;

    private LocalDateTime payTime;

    private LocalDateTime merchantAcceptTime;

    private LocalDateTime riderTakeTime;

    private LocalDateTime deliverTime;

    private LocalDateTime completeTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private Long deliveryManId;

    private Long deliveryTaskId;

    private LocalDateTime expectedTime;

    private Integer isRated;

    private Integer source;

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
