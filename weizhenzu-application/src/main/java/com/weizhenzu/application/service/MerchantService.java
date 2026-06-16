package com.weizhenzu.application.service;

import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.domain.dto.MerchantRegisterDTO;
import com.weizhenzu.domain.vo.DishCategoryVO;
import com.weizhenzu.domain.vo.MerchantCategoryVO;
import com.weizhenzu.domain.vo.MerchantVO;

import java.util.List;

/**
 * 商家服务接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface MerchantService {

    /**
     * 商家注册
     */
    Long register(MerchantRegisterDTO dto);

    /**
     * 商家详情
     */
    MerchantVO detail(Long id);

    /**
     * 当前商家信息
     */
    MerchantVO current();

    /**
     * 商家列表（C端）
     */
    PageResult<MerchantVO> userPage(Integer current, Integer size, Long categoryId, String keyword);

    /**
     * 商家菜单（C端）
     */
    List<DishCategoryVO> menu(Long merchantId);

    /**
     * 商家分类列表（C端）
     */
    List<MerchantCategoryVO> categories();
}
