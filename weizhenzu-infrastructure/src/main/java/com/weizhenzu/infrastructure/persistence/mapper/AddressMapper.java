package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 收货地址 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {

    /**
     * 取消其他默认地址
     */
    @Update("UPDATE t_address SET is_default = 0, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND is_default = 1 AND deleted = 0")
    int clearDefault(@Param("userId") Long userId);
}
