package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改购物车 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "修改购物车请求")
public class CartUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数量（0表示删除）")
    @NotNull(message = "数量不能为空")
    private Integer quantity;
}
