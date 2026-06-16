package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
