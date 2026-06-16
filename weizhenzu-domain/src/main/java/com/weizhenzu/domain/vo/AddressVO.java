package com.weizhenzu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 收货地址 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "收货地址")
public class AddressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "地址ID")
    private Long id;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人手机号（脱敏）")
    private String contactPhone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区")
    private String district;

    @Schema(description = "详细地址")
    private String detail;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "标签")
    private String tag;

    @Schema(description = "是否默认：0否 1是")
    private Integer isDefault;
}
