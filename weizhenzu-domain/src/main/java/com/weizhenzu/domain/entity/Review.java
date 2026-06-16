package com.weizhenzu.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_review")
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private Long merchantId;

    private Long deliveryManId;

    private Integer rating;

    private Integer tasteScore;

    private Integer packingScore;

    private Integer deliveryScore;

    private String content;

    private String images;

    private String tags;

    private Integer anonymous;

    private String merchantReply;

    private LocalDateTime merchantReplyTime;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(value = "deleted", select = false)
    private Integer deleted;
}
