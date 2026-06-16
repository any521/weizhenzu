package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.SmsLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短信日志 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface SmsLogMapper extends BaseMapper<SmsLog> {
}
