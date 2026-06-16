package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.OrderLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态流转日志 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface OrderLogMapper extends BaseMapper<OrderLog> {
}
