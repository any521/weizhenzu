package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色权限关联 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
