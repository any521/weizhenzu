package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 菜品 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 乐观锁扣库存
     *
     * @param dishId   菜品ID
     * @param quantity 扣减数量
     * @return 影响行数
     */
    @Update("UPDATE t_dish SET stock = stock - #{quantity}, total_sales = total_sales + #{quantity}, " +
            "month_sales = month_sales + #{quantity}, version = version + 1, updated_at = NOW() " +
            "WHERE id = #{dishId} AND deleted = 0 AND status = 1 " +
            "AND (stock = -1 OR stock >= #{quantity})")
    int deductStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    /**
     * 回退库存
     */
    @Update("UPDATE t_dish SET stock = CASE WHEN stock = -1 THEN -1 ELSE stock + #{quantity} END, " +
            "total_sales = GREATEST(total_sales - #{quantity}, 0), " +
            "month_sales = GREATEST(month_sales - #{quantity}, 0), " +
            "version = version + 1, updated_at = NOW() WHERE id = #{dishId}")
    int rollbackStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);
}
