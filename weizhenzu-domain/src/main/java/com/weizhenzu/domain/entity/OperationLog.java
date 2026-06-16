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
 * 操作日志
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Integer operatorType;

    private Long operatorId;

    private String module;

    private String action;

    private String method;

    private String params;

    private Integer result;

    private String errorMsg;

    private String ip;

    private String userAgent;

    private Integer costMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
