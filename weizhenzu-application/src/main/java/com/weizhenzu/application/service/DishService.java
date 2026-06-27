package com.weizhenzu.application.service;

import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.domain.dto.DishDTO;
import com.weizhenzu.domain.vo.DishVO;

import java.util.List;

/**
 * 菜品服务接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface DishService {

    /**
     * 菜品详情（C端）
     */
    DishVO detail(Long id);

    /**
     * 商家菜品详情
     */
    DishVO merchantDetail(Long id);

    /**
     * 商家新增菜品
     */
    Long add(DishDTO dto);

    /**
     * 商家修改菜品
     */
    void update(Long id, DishDTO dto);

    /**
     * 商家上下架菜品
     */
    void updateStatus(Long id, Integer status);

    /**
     * 商家删除菜品
     */
    void delete(Long id);

    /**
     * 商家菜品分页
     */
    PageResult<DishVO> merchantPage(Integer current, Integer size, Long categoryId, String keyword);

    /**
     * C端菜品搜索/分类列表（按平台分类）
     */
    PageResult<DishVO> userPage(Integer current, Integer size, Long platformCategoryId, String keyword);

    /**
     * C端精选菜品（最近上架，最多limit道）
     *
     * @param limit      数量，默认5
     * @param diningType 用餐类型: 2=外卖, 3=自取；为null时不过滤（返回全部）
     */
    List<DishVO> featuredDishes(Integer limit, Integer diningType);
}
