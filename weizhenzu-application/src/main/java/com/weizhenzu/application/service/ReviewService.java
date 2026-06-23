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
     * 我的评价列表（C端）
     */
    PageResult<ReviewVO> userPage(Integer current, Integer size);

    /**
     * 商家评价列表（C端，按商家ID查询）
     */
    PageResult<ReviewVO> merchantReviews(Long merchantId, Integer current, Integer size);

    /**
     * 菜品评价列表（C端，按菜品ID查询）
     */
    PageResult<ReviewVO> dishReviews(Long dishId, Integer current, Integer size);

    /**
     * 商家回复评价
     */
    void reply(Long id, String content);

    /**
     * 商家更新评价状态（隐藏/公开）
     *
     * @param id     评价ID
     * @param status 1公开 0隐藏
     */
    void updateStatus(Long id, Integer status);
}
