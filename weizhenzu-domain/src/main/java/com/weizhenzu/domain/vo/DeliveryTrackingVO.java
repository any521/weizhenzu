package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送跟踪 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "配送跟踪")
public class DeliveryTrackingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "订单状态描述")
    private String statusDesc;

    @Schema(description = "配送步骤")
    private List<Step> steps;

    @Schema(description = "骑手信息")
    private RiderInfo rider;

    @Schema(description = "商家信息")
    private MerchantInfo merchant;

    @Schema(description = "用户信息")
    private UserInfo userInfo;

    @Schema(description = "骑手经度")
    private BigDecimal riderLng;

    @Schema(description = "骑手纬度")
    private BigDecimal riderLat;

    @Schema(description = "商家经度")
    private BigDecimal merchantLng;

    @Schema(description = "商家纬度")
    private BigDecimal merchantLat;

    @Schema(description = "用户经度")
    private BigDecimal userLng;

    @Schema(description = "用户纬度")
    private BigDecimal userLat;

    @Schema(description = "当前导航目标距离（米）：取餐前为骑手到商家距离，取餐后为骑手到用户距离")
    private Integer distance;

    @Schema(description = "骑手到商家距离（米）")
    private Integer distanceToMerchant;

    @Schema(description = "骑手到用户距离（米）")
    private Integer distanceToUser;

    @Schema(description = "当前导航目标：merchant=前往商家，user=前往用户")
    private String navigationTarget;

    @Schema(description = "用餐类型：1=堂食，2=外卖")
    private Integer diningType;

    @Schema(description = "骑手最近留言列表（最多5条）")
    private List<RiderMessage> recentMessages;

    /**
     * 配送步骤
     */
    @Data
    @Schema(description = "配送步骤")
    public static class Step implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "步骤名称")
        private String name;

        @Schema(description = "时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime time;

        @Schema(description = "是否完成")
        private Boolean done;
    }

    /**
     * 骑手信息
     */
    @Data
    @Schema(description = "骑手信息")
    public static class RiderInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "骑手ID")
        private Long id;

        @Schema(description = "姓名")
        private String name;

        @Schema(description = "头像")
        private String avatar;

        @Schema(description = "电话（脱敏）")
        private String phone;

        @Schema(description = "评分")
        private java.math.BigDecimal rating;

        @Schema(description = "骑手经度")
        private BigDecimal lng;

        @Schema(description = "骑手纬度")
        private BigDecimal lat;

        @Schema(description = "距用户距离（米）")
        private Integer distance;
    }

    /**
     * 商家信息
     */
    @Data
    @Schema(description = "商家信息")
    public static class MerchantInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "商家名称")
        private String name;

        @Schema(description = "商家地址")
        private String address;

        @Schema(description = "商家电话（脱敏）")
        private String phone;

        @Schema(description = "商家经度")
        private BigDecimal lng;

        @Schema(description = "商家纬度")
        private BigDecimal lat;
    }

    /**
     * 用户信息
     */
    @Data
    @Schema(description = "用户信息")
    public static class UserInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "用户收货地址")
        private String address;

        @Schema(description = "用户经度")
        private BigDecimal lng;

        @Schema(description = "用户纬度")
        private BigDecimal lat;

        @Schema(description = "用户联系电话（脱敏）")
        private String phone;
    }

    /**
     * 骑手留言
     */
    @Data
    @Schema(description = "骑手留言")
    public static class RiderMessage implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "留言ID")
        private Long id;

        @Schema(description = "留言内容")
        private String content;

        @Schema(description = "留言时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime time;
    }
}
