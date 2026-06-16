package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Refund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 退款单 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface RefundMapper extends BaseMapper<Refund> {

    /**
     * 更新退款状态
     */
    @Update("UPDATE t_refund SET status = #{status}, " +
            "audit_remark = #{auditRemark}, auditor_id = #{auditorId}, " +
            "refund_time = CASE WHEN #{status} = 5 THEN NOW() ELSE refund_time END, " +
            "updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("auditRemark") String auditRemark,
                     @Param("auditorId") Long auditorId);
}
