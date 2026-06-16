package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券模板 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 领取优惠券（乐观锁扣减剩余量）
     */
    @Update("UPDATE t_coupon SET received_count = received_count + 1, updated_at = NOW() " +
            "WHERE id = #{couponId} AND deleted = 0 AND status = 1 " +
            "AND received_count < total_count")
    int increaseReceived(@Param("couponId") Long couponId);
}
