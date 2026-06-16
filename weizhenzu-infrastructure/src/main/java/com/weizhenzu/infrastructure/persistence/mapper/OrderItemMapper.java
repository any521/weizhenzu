package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
