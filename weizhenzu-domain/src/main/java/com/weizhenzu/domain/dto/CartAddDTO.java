package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 加入购物车 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "加入购物车请求")
public class CartAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商家ID")
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @Schema(description = "菜品ID")
    @NotNull(message = "菜品ID不能为空")
    private Long dishId;

    @Schema(description = "规格ID")
    private Long specId;

    @Schema(description = "数量")
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;
}
