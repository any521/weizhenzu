package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Settlement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家结算单 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface SettlementMapper extends BaseMapper<Settlement> {
}
