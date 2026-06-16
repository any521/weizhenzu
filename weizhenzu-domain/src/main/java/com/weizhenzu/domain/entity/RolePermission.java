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
 * 角色权限关联
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@TableName("t_role_permission")
public class RolePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long roleId;

    private Long permissionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
