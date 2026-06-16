package com.weizhenzu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品规格 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "菜品规格")
public class DishSpecVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "规格ID")
    private Long id;

    @Schema(description = "菜品ID")
    private Long dishId;

    @Schema(description = "规格名")
    private String name;

    @Schema(description = "加价")
    private BigDecimal priceDiff;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "状态：0下架 1上架")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;
}
