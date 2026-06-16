package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}
