package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商家 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    /**
     * 根据手机号哈希查询商家
     */
    @Select("SELECT * FROM t_merchant WHERE phone_hash = #{phoneHash} AND deleted = 0 LIMIT 1")
    Merchant selectByPhoneHash(@Param("phoneHash") String phoneHash);
}
