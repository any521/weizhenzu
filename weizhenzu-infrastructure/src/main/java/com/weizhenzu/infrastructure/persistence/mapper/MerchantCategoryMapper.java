package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.MerchantCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家类目 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface MerchantCategoryMapper extends BaseMapper<MerchantCategory> {
}
