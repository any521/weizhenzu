package com.weizhenzu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜品 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "菜品信息")
public class DishVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品ID")
    private Long id;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "商家菜品分类ID（商家自定义分类）")
    private Long categoryId;

    @Schema(description = "商家菜品分类名称")
    private String categoryName;

    @Schema(description = "平台分类ID（平台级分类标签）")
    private Long platformCategoryId;

    @Schema(description = "平台分类名称")
    private String platformCategoryName;

    @Schema(description = "菜品名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "主图")
    private String image;

    @Schema(description = "图片列表")
    private List<String> images;

    @Schema(description = "现价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "库存（-1无限）")
    private Integer stock;

    @Schema(description = "月售")
    private Integer monthSales;

    @Schema(description = "总售")
    private Integer totalSales;

    @Schema(description = "评分")
    private BigDecimal rating;

    @Schema(description = "标签")
    private List<String> tags;

    @Schema(description = "辣度0-3")
    private Integer spicy;

    @Schema(description = "状态：0下架 1上架")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "规格列表")
    private List<DishSpecVO> specs;
}
