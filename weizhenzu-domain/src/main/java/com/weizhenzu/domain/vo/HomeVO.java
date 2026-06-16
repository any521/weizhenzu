package com.weizhenzu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 首页聚合数据 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "首页聚合数据")
public class HomeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "定位地址")
    private String location;

    @Schema(description = "商家类目列表")
    private List<Category> categories;

    @Schema(description = "Banner列表")
    private List<Banner> banners;

    @Schema(description = "推荐商家列表")
    private List<MerchantVO> merchants;

    /**
     * 类目
     */
    @Data
    @Schema(description = "商家类目")
    public static class Category implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "类目ID")
        private Long id;

        @Schema(description = "类目名称")
        private String name;

        @Schema(description = "图标")
        private String icon;
    }

    /**
     * Banner
     */
    @Data
    @Schema(description = "Banner")
    public static class Banner implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "BannerID")
        private Long id;

        @Schema(description = "图片")
        private String image;

        @Schema(description = "链接")
        private String link;
    }
}
