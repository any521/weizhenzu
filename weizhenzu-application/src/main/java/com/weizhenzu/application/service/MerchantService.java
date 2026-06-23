package com.weizhenzu.application.service;

import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.domain.dto.MerchantRegisterDTO;
import com.weizhenzu.domain.vo.DishCategoryVO;
import com.weizhenzu.domain.vo.MerchantCategoryVO;
import com.weizhenzu.domain.vo.MerchantVO;

import java.math.BigDecimal;
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

    /**
     * 更新商家设置
     *
     * @param name            店铺名称
     * @param logo            店铺Logo
     * @param description     简介
     * @param notice          公告
     * @param openTime        营业时间
     * @param isOpen          是否营业
     * @param minOrderAmount  起送价
     * @param deliveryFee     配送费
     * @param packingFee      打包费
     * @param deliveryRadius  配送半径
     */
    void updateSettings(String name, String logo, String description, String notice,
                        String openTime, Integer isOpen, BigDecimal minOrderAmount,
                        BigDecimal deliveryFee, BigDecimal packingFee, Integer deliveryRadius);

    /**
     * 商家财务统计
     */
    java.util.Map<String, Object> financeStats();

    /**
     * 商家财务图表数据
     */
    java.util.Map<String, Object> financeChart();
}
