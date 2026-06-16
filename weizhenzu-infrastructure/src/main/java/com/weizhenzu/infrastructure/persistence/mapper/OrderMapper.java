package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 订单 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 乐观锁更新状态
     */
    @Update("UPDATE t_order SET status = #{toStatus}, version = version + 1, " +
            "updated_at = NOW() WHERE id = #{orderId} AND status = #{fromStatus} AND deleted = 0")
    int updateStatus(@Param("orderId") Long orderId,
                     @Param("fromStatus") Integer fromStatus,
                     @Param("toStatus") Integer toStatus);

    /**
     * 更新支付状态
     */
    @Update("UPDATE t_order SET pay_status = #{payStatus}, pay_time = NOW(), " +
            "version = version + 1, updated_at = NOW() WHERE id = #{orderId} AND deleted = 0")
    int updatePayStatus(@Param("orderId") Long orderId, @Param("payStatus") Integer payStatus);
}
