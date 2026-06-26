package com.weizhenzu.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_dish")
public class Dish implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long merchantId;

    /**
     * 商家菜品分类ID（关联 t_dish_category.id）
     */
    private Long categoryId;

    /**
     * 平台分类ID（关联 t_merchant_category.id，用于平台级标签归类）
     */
    private Long platformCategoryId;

    private String name;

    private String description;

    private String image;

    private String images;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer monthSales;

    private Integer totalSales;

    private BigDecimal rating;

    private String tags;

    private Integer spicy;

    private Integer status;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(value = "deleted", select = false)
    private Integer deleted;

    @Version
    private Integer version;
}
