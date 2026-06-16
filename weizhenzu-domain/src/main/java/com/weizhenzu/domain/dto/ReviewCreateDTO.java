package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建评价 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "创建评价请求")
public class ReviewCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "总评分1-5")
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小1")
    @Max(value = 5, message = "评分最大5")
    private Integer rating;

    @Schema(description = "口味评分1-5")
    @Min(value = 1, message = "评分最小1")
    @Max(value = 5, message = "评分最大5")
    private Integer tasteScore;

    @Schema(description = "包装评分1-5")
    @Min(value = 1, message = "评分最小1")
    @Max(value = 5, message = "评分最大5")
    private Integer packingScore;

    @Schema(description = "配送评分1-5")
    @Min(value = 1, message = "评分最小1")
    @Max(value = 5, message = "评分最大5")
    private Integer deliveryScore;

    @Schema(description = "评价内容")
    @Size(max = 500, message = "评价内容最多500字")
    private String content;

    @Schema(description = "图片列表")
    private List<String> images;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "是否匿名：0否 1是")
    private Integer anonymous;
}
