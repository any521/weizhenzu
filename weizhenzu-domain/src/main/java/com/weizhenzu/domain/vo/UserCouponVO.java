package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户优惠券 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "用户优惠券")
public class UserCouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户优惠券ID")
    private Long id;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "类型：1满减 2折扣 3无门槛")
    private Integer type;

    @Schema(description = "满减金额")
    private BigDecimal amount;

    @Schema(description = "使用门槛")
    private BigDecimal threshold;

    @Schema(description = "折扣")
    private BigDecimal discount;

    @Schema(description = "最大优惠")
    private BigDecimal maxDiscount;

    @Schema(description = "状态：0未使用 1已使用 2已过期")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "有效期开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validStart;

    @Schema(description = "有效期结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validEnd;

    @Schema(description = "领取时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "适用范围：1全场通用 2指定商家 3指定类目")
    private Integer scope;

    @Schema(description = "适用范围描述")
    private String scopeDesc;

    @Schema(description = "适用商家/类目ID列表（字符串形式，避免前端JS精度丢失）")
    private List<String> scopeIds;
}
