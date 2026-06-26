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
     * 更新支付状态（乐观锁：要求当前状态不为终态，避免并发回调重复更新）
     */
    @Update("UPDATE t_payment SET status = #{status}, third_party_no = #{thirdPartyNo}, " +
            "paid_time = CASE WHEN #{status} = 1 THEN NOW() ELSE paid_time END, " +
            "updated_at = NOW() WHERE id = #{id} AND status = #{expectedStatus}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("thirdPartyNo") String thirdPartyNo,
                     @Param("expectedStatus") Integer expectedStatus);

    /**
     * 更新支付状态（不带乐观锁，用于失败/关闭等场景）
     */
    @Update("UPDATE t_payment SET status = #{status}, third_party_no = #{thirdPartyNo}, " +
            "updated_at = NOW() WHERE id = #{id}")
    int updateStatusForce(@Param("id") Long id,
                          @Param("status") Integer status,
                          @Param("thirdPartyNo") String thirdPartyNo);
}
