package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建订单 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "创建订单请求")
public class OrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商家ID")
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @Schema(description = "收货地址ID")
    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;

    @Schema(description = "订单明细")
    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<OrderItemDTO> items;

    @Schema(description = "用户优惠券ID")
    private Long userCouponId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "用餐类型：1=堂食，2=外卖，默认外卖")
    private Integer diningType;

    @Schema(description = "幂等令牌（防重复下单）")
    @NotEmpty(message = "clientToken不能为空")
    private String clientToken;

    /**
     * 订单明细
     */
    @Data
    @Schema(description = "订单明细")
    public static class OrderItemDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "菜品ID")
        @NotNull(message = "菜品ID不能为空")
        private Long dishId;

        @Schema(description = "规格ID")
        private Long specId;

        @Schema(description = "数量")
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于0")
        private Integer quantity;
    }
}
