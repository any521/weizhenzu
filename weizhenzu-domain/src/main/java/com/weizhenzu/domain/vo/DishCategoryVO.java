package com.weizhenzu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜品分类 VO（含分类下菜品）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "菜品分类")
public class DishCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：0禁用 1启用")
    private Integer status;

    @Schema(description = "分类下菜品列表")
    private List<DishVO> dishes;
}
