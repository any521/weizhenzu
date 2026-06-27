package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单响应 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "创建订单响应")
public class OrderCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "订单状态描述")
    private String statusDesc;

    @Schema(description = "商品总金额")
    private BigDecimal totalAmount;

    @Schema(description = "打包费")
    private BigDecimal packingFee;

    @Schema(description = "配送费")
    private BigDecimal deliveryFee;

    @Schema(description = "优惠金额")
    private BigDecimal couponAmount;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "用餐类型：1=堂食，2=外卖，3=自取")
    private Integer diningType;

    @Schema(description = "支付过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
