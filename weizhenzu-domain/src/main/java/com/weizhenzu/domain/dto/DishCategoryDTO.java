package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜品分类 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "菜品分类请求")
public class DishCategoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称最多50字")
    private String name;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：0禁用 1启用")
    private Integer status;
}
