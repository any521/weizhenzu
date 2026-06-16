package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 支付记录 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 更新支付状态
     */
    @Update("UPDATE t_payment SET status = #{status}, third_party_no = #{thirdPartyNo}, " +
            "paid_time = CASE WHEN #{status} = 1 THEN NOW() ELSE paid_time END, " +
            "updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("thirdPartyNo") String thirdPartyNo);
}
