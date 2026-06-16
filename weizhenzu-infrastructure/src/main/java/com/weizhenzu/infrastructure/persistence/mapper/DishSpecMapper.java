package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.DishSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 菜品规格 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface DishSpecMapper extends BaseMapper<DishSpec> {

    /**
     * 乐观锁扣规格库存
     */
    @Update("UPDATE t_dish_spec SET stock = stock - #{quantity}, updated_at = NOW() " +
            "WHERE id = #{specId} AND deleted = 0 AND status = 1 " +
            "AND (stock = -1 OR stock >= #{quantity})")
    int deductStock(@Param("specId") Long specId, @Param("quantity") Integer quantity);
}
