package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
    Merchant selectByPhoneHashRaw(@Param("phoneHash") String phoneHash);

    /**
     * 根据ID查询商家（绕过 @TableLogic，直接查库）
     */
    Merchant selectByIdRaw(@Param("id") Long id);
}
