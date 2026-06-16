package com.weizhenzu.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_message")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Integer userType;

    private String title;

    private String content;

    private Integer type;

    private String bizType;

    private Long bizId;

    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
