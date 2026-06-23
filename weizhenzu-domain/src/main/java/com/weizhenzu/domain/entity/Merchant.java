package com.weizhenzu.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商家实体
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_merchant")
public class Merchant extends BaseEntity {

    private String phone;
    private String phoneHash;
    private String password;
    private String name;
    private String logo;
    private Long categoryId;
    private String description;
    private String notice;
    private String contactName;
    private String contactPhone;
    private String province;
    private String city;
    private String district;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer deliveryRadius;
    private BigDecimal minOrderAmount;
    private BigDecimal deliveryFee;
    private BigDecimal packingFee;
    private String openTime;
    private Integer isOpen;
    private Integer status;
    private String auditRemark;
    private BigDecimal rating;
    private Integer monthSales;
    private String qualification;
}
