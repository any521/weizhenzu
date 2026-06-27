package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "评价信息")
public class ReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评价ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "骑手ID")
    private Long deliveryManId;

    @Schema(description = "骑手名称")
    private String deliveryManName;

    @Schema(description = "评价菜品名称列表")
    private List<String> dishNames;

    @Schema(description = "总评分")
    private Integer rating;

    @Schema(description = "口味评分")
    private Integer tasteScore;

    @Schema(description = "包装评分")
    private Integer packingScore;

    @Schema(description = "配送评分")
    private Integer deliveryScore;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "图片列表")
    private List<String> images;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "是否匿名")
    private Integer anonymous;

    @Schema(description = "商家回复")
    private String merchantReply;

    @Schema(description = "商家回复（别名，兼容前端）")
    private String reply;

    @Schema(description = "商家回复时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime merchantReplyTime;

    @Schema(description = "评价状态：0隐藏 1公开")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
