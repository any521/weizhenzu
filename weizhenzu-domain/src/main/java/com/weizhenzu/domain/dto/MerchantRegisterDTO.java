package com.weizhenzu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家注册 DTO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "商家注册请求")
public class MerchantRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "登录手机号")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度6-32位")
    private String password;

    @Schema(description = "店铺名称")
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称最多100字")
    private String name;

    @Schema(description = "商家类目ID")
    @NotNull(message = "商家类目不能为空")
    private Long categoryId;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

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

    @Schema(description = "店铺简介")
    private String description;

    @Schema(description = "店铺公告")
    private String notice;
}
