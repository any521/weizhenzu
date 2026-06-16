package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Review;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
