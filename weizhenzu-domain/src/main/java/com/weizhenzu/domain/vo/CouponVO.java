package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "优惠券")
public class CouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "优惠券ID")
    private Long id;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "类型：1满减 2折扣 3无门槛")
    private Integer type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "满减金额")
    private BigDecimal amount;

    @Schema(description = "使用门槛")
    private BigDecimal threshold;

    @Schema(description = "折扣")
    private BigDecimal discount;

    @Schema(description = "最大优惠")
    private BigDecimal maxDiscount;

    @Schema(description = "发放总量")
    private Integer totalCount;

    @Schema(description = "已领取")
    private Integer receivedCount;

    @Schema(description = "已使用")
    private Integer usedCount;

    @Schema(description = "每人限领")
    private Integer perLimit;

    @Schema(description = "有效期类型：1固定时间 2领取后N天")
    private Integer validType;

    @Schema(description = "有效期开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validStart;

    @Schema(description = "有效期结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validEnd;

    @Schema(description = "领取后N天生效")
    private Integer validDays;

    @Schema(description = "适用范围：1全场 2指定商家 3指定类目")
    private Integer scope;

    @Schema(description = "状态：0禁用 1启用")
    private Integer status;

    @Schema(description = "是否可领取")
    private Boolean canReceive;
}
