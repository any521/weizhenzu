package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.DeliveryTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 配送任务 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface DeliveryTaskMapper extends BaseMapper<DeliveryTask> {

    /**
     * 抢单（乐观锁）
     */
    @Update("UPDATE t_delivery_task SET delivery_man_id = #{riderId}, status = 1, " +
            "grab_time = NOW(), updated_at = NOW() " +
            "WHERE id = #{taskId} AND status = 0 AND delivery_man_id IS NULL")
    int grab(@Param("taskId") Long taskId, @Param("riderId") Long riderId);

    /**
     * 更新任务状态
     */
    @Update("UPDATE t_delivery_task SET status = #{status}, updated_at = NOW() " +
            "WHERE id = #{taskId}")
    int updateStatus(@Param("taskId") Long taskId, @Param("status") Integer status);
}
