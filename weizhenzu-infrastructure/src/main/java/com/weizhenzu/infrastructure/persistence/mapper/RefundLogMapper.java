package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.RefundLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款流水 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface RefundLogMapper extends BaseMapper<RefundLog> {
}
