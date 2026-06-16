package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
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
    }
}
