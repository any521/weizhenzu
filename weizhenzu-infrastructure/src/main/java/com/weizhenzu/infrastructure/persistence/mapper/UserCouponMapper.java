package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户优惠券 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 标记已使用
     */
    @Update("UPDATE t_user_coupon SET status = 1, order_id = #{orderId}, used_time = NOW(), " +
            "updated_at = NOW() WHERE id = #{id} AND status = 0")
    int markUsed(@Param("id") Long id, @Param("orderId") Long orderId);

    /**
     * 标记过期
     */
    @Update("UPDATE t_user_coupon SET status = 2, updated_at = NOW() " +
            "WHERE status = 0 AND valid_end < NOW()")
    int markExpired();
}
