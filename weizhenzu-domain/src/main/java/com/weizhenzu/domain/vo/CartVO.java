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

    @Schema(description = "商家ID（默认取第一个商家，兼容旧前端）")
    private Long merchantId;

    @Schema(description = "商家名称（默认取第一个商家，兼容旧前端）")
    private String merchantName;

    @Schema(description = "购物车项列表（默认取第一个商家的商品，兼容旧前端）")
    private List<CartItemVO> items;

    @Schema(description = "商品总额（所有商家合计）")
    private BigDecimal totalAmount;

    @Schema(description = "配送费（默认取第一个商家，兼容旧前端）")
    private BigDecimal deliveryFee;

    @Schema(description = "打包费（默认取第一个商家，兼容旧前端）")
    private BigDecimal packingFee;

    @Schema(description = "应付金额（默认取第一个商家，兼容旧前端）")
    private BigDecimal payAmount;

    @Schema(description = "起送价（默认取第一个商家，兼容旧前端）")
    private BigDecimal minOrderAmount;

    @Schema(description = "是否达到起送价（默认取第一个商家，兼容旧前端）")
    private Boolean reachMinAmount;

    @Schema(description = "商品总数量")
    private Integer totalCount;

    @Schema(description = "商家分组列表（多商家购物车）")
    private List<CartGroupVO> groups;
}
