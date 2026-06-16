package com.weizhenzu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "购物车")
public class CartVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "购物车项列表")
    private List<CartItemVO> items;

    @Schema(description = "商品总额")
    private BigDecimal totalAmount;

    @Schema(description = "配送费")
    private BigDecimal deliveryFee;

    @Schema(description = "打包费")
    private BigDecimal packingFee;

    @Schema(description = "应付金额")
    private BigDecimal payAmount;

    @Schema(description = "起送价")
    private BigDecimal minOrderAmount;

    @Schema(description = "是否达到起送价")
    private Boolean reachMinAmount;
}
