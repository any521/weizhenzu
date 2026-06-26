package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "订单详情")
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userName;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "商家Logo")
    private String merchantLogo;

    @Schema(description = "商家电话")
    private String merchantPhone;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "订单状态描述")
    private String statusDesc;

    @Schema(description = "支付状态")
    private Integer payStatus;

    @Schema(description = "支付状态描述")
    private String payStatusDesc;

    @Schema(description = "商品总数")
    private Integer itemCount;

    @Schema(description = "商品总额")
    private BigDecimal totalAmount;

    @Schema(description = "打包费")
    private BigDecimal packingFee;

    @Schema(description = "配送费")
    private BigDecimal deliveryFee;

    @Schema(description = "商家优惠")
    private BigDecimal merchantDiscount;

    @Schema(description = "平台优惠")
    private BigDecimal platformDiscount;

    @Schema(description = "优惠券抵扣")
    private BigDecimal couponAmount;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "支付方式：1支付宝 2微信 3余额")
    private Integer payType;

    @Schema(description = "收货地址快照")
    private AddressSnapshotVO address;

    @Schema(description = "订单明细")
    private List<OrderItemVO> items;

    @Schema(description = "支付时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @Schema(description = "商家接单时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime merchantAcceptTime;

    @Schema(description = "骑手接单时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime riderTakeTime;

    @Schema(description = "送达时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliverTime;

    @Schema(description = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    @Schema(description = "取消时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "预计送达")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedTime;

    @Schema(description = "配送任务ID")
    private Long deliveryTaskId;

    @Schema(description = "配送任务状态：0待抢1已抢2到店3取餐4配送中5已送达6取消")
    private Integer deliveryTaskStatus;

    @Schema(description = "商家地址（骑手端显示用）")
    private String merchantAddress;

    @Schema(description = "商家经度")
    private BigDecimal merchantLng;

    @Schema(description = "商家纬度")
    private BigDecimal merchantLat;

    @Schema(description = "用户地址")
    private String userAddress;

    @Schema(description = "用户经度")
    private BigDecimal userLng;

    @Schema(description = "用户纬度")
    private BigDecimal userLat;

    @Schema(description = "是否已评价")
    private Integer isRated;

    @Schema(description = "用餐类型：1=堂食，2=外卖")
    private Integer diningType;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 订单明细
     */
    @Data
    @Schema(description = "订单明细")
    public static class OrderItemVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "明细ID")
        private Long id;

        @Schema(description = "菜品ID")
        private Long dishId;

        @Schema(description = "菜品名称")
        private String dishName;

        @Schema(description = "菜品图片")
        private String dishImage;

        @Schema(description = "规格ID")
        private Long specId;

        @Schema(description = "规格名称")
        private String specName;

        @Schema(description = "单价")
        private BigDecimal unitPrice;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "小计")
        private BigDecimal subtotal;
    }

    /**
     * 地址快照
     */
    @Data
    @Schema(description = "收货地址快照")
    public static class AddressSnapshotVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "联系人")
        private String contactName;

        @Schema(description = "联系电话（脱敏）")
        private String contactPhone;

        @Schema(description = "省")
        private String province;

        @Schema(description = "市")
        private String city;

        @Schema(description = "区")
        private String district;

        @Schema(description = "详细地址")
        private String detail;

        @Schema(description = "完整地址")
        private String fullAddress;
    }
}
