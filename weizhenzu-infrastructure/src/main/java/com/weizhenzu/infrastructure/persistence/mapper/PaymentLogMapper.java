package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.PaymentLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付流水 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface PaymentLogMapper extends BaseMapper<PaymentLog> {
}
