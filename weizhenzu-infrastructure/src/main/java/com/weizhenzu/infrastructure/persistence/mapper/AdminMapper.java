package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {

    /**
     * 根据用户名查询管理员
     */
    @Select("SELECT * FROM t_admin WHERE username = #{username} AND deleted = 0 LIMIT 1")
    Admin selectByUsername(@Param("username") String username);

    /**
     * 根据手机号哈希查询管理员
     */
    @Select("SELECT * FROM t_admin WHERE phone_hash = #{phoneHash} AND deleted = 0 LIMIT 1")
    Admin selectByPhoneHash(@Param("phoneHash") String phoneHash);
}
