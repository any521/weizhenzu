package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询基础 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "分页查询请求")
public class PageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小1")
    private Integer current = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数最小1")
    @Max(value = 100, message = "每页条数最大100")
    private Integer size = 10;

    @Schema(description = "搜索关键词")
    private String keyword;
}
