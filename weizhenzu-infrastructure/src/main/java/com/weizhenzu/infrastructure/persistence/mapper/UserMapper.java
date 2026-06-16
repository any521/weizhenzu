package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * C端用户 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据手机号哈希查询用户
     */
    @Select("SELECT * FROM t_user WHERE phone_hash = #{phoneHash} AND deleted = 0 LIMIT 1")
    User selectByPhoneHash(@Param("phoneHash") String phoneHash);
}
