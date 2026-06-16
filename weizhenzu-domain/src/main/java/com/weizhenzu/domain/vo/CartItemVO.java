package com.weizhenzu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车项 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "购物车项")
public class CartItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车ID")
    private Long id;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "菜品ID")
    private Long dishId;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "菜品图片")
    private String dishImage;

    @Schema(description = "规格ID")
    private Long specId;

    @Schema(description = "规格名称")
    private String specName;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "小计")
    private BigDecimal subtotal;
}
