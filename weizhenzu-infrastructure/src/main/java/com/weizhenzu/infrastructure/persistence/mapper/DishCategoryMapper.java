package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.DishCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品分类 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface DishCategoryMapper extends BaseMapper<DishCategory> {
}
