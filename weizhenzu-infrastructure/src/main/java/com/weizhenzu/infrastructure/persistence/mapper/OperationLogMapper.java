package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
