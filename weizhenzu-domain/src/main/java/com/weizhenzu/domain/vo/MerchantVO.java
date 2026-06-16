package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家信息 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "商家信息")
public class MerchantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商家ID")
    private Long id;

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "店铺Logo")
    private String logo;

    @Schema(description = "商家类目ID")
    private Long categoryId;

    @Schema(description = "商家类目名称")
    private String categoryName;

    @Schema(description = "店铺简介")
    private String description;

    @Schema(description = "店铺公告")
    private String notice;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "配送半径(米)")
    private Integer deliveryRadius;

    @Schema(description = "起送价")
    private BigDecimal minOrderAmount;

    @Schema(description = "配送费")
    private BigDecimal deliveryFee;

    @Schema(description = "打包费")
    private BigDecimal packingFee;

    @Schema(description = "营业时间")
    private String openTime;

    @Schema(description = "是否营业")
    private Integer isOpen;

    @Schema(description = "状态：0待审核 1正常 2禁用 3驳回")
    private Integer status;

    @Schema(description = "评分")
    private BigDecimal rating;

    @Schema(description = "月售")
    private Integer monthSales;

    @Schema(description = "距离(米)")
    private Integer distance;

    @Schema(description = "预计送达时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedTime;
}
