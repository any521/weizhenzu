package com.weizhenzu.application.service;

import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.domain.dto.ReviewCreateDTO;
import com.weizhenzu.domain.vo.ReviewVO;

/**
 * 评价服务接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface ReviewService {

    /**
     * 创建评价
     */
    Long create(ReviewCreateDTO dto);

    /**
     * 评价详情
     */
    ReviewVO detail(Long id);

    /**
     * 商家评价列表
     */
    PageResult<ReviewVO> merchantPage(Integer current, Integer size, Integer rating);

    /**
     * 商家回复评价
     */
    void reply(Long id, String content);
}
