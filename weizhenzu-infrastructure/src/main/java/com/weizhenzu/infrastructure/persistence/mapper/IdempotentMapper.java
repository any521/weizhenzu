package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Idempotent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等记录 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface IdempotentMapper extends BaseMapper<Idempotent> {
}
