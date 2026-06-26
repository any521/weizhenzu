package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品 DTO（商家新增/修改）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "菜品请求")
public class DishDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品分类ID（商家自定义分类）")
    @NotNull(message = "菜品分类不能为空")
    private Long categoryId;

    @Schema(description = "平台分类ID（平台级分类标签，可选）")
    private Long platformCategoryId;

    @Schema(description = "菜品名称")
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 100, message = "菜品名称最多100字")
    private String name;

    @Schema(description = "描述")
    @Size(max = 500, message = "描述最多500字")
    private String description;

    @Schema(description = "主图")
    private String image;

    @Schema(description = "图片列表JSON")
    private String images;

    @Schema(description = "现价")
    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "库存（-1无限）")
    private Integer stock;

    @Schema(description = "标签JSON")
    private String tags;

    @Schema(description = "辣度0-3")
    private Integer spicy;

    @Schema(description = "状态：0下架 1上架")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;
}
