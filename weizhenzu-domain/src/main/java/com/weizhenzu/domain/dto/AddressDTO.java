package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 收货地址 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "收货地址请求")
public class AddressDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "联系人姓名")
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @Schema(description = "联系人手机号")
    @NotBlank(message = "联系人手机号不能为空")
    private String contactPhone;

    @Schema(description = "省")
    @NotBlank(message = "省不能为空")
    private String province;

    @Schema(description = "市")
    @NotBlank(message = "市不能为空")
    private String city;

    @Schema(description = "区")
    @NotBlank(message = "区不能为空")
    private String district;

    @Schema(description = "详细地址")
    @NotBlank(message = "详细地址不能为空")
    private String detail;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "标签：家/公司/学校")
    private String tag;

    @Schema(description = "是否默认：0否 1是")
    private Integer isDefault;
}
