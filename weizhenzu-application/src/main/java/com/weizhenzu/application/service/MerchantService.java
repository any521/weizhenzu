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
     * @param id 商家ID
     * @param lng 用户经度（可选，用于计算距离）
     * @param lat 用户纬度（可选，用于计算距离）
     */
    MerchantVO detail(Long id, BigDecimal lng, BigDecimal lat);

    /**
     * 当前商家信息
     */
    MerchantVO current();

    /**
     * 商家列表（C端）
     *
     * @param current      当前页
     * @param size         每页大小
     * @param categoryId   分类ID
     * @param keyword      搜索关键词
     * @param deliveryType 配送类型：1=外卖，2=自取，null=全部
     * @param lng          用户经度（可选，用于计算距离和排序）
     * @param lat          用户纬度（可选，用于计算距离和排序）
     */
    PageResult<MerchantVO> userPage(Integer current, Integer size, Long categoryId, String keyword, Integer deliveryType,
                                    BigDecimal lng, BigDecimal lat);

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
     * @param contactPerson   联系人
     * @param phone           联系电话
     * @param description     简介
     * @param notice          公告
     * @param openTime        营业时间
     * @param isOpen          是否营业
     * @param minOrderAmount  起送价
     * @param deliveryFee     配送费
     * @param packingFee      打包费
     * @param deliveryRadius  配送半径
     * @param supportDelivery 是否支持外卖
     * @param supportPickup   是否支持自取
     * @param longitude       经度
     * @param latitude        纬度
     */
    void updateSettings(String name, String logo, String contactPerson, String phone, String description, String notice,
                        String openTime, String address, Integer isOpen, BigDecimal minOrderAmount,
                        BigDecimal deliveryFee, BigDecimal packingFee, Integer deliveryRadius,
                        Integer supportDelivery, Integer supportPickup, BigDecimal longitude, BigDecimal latitude);

    /**
     * 商家财务统计
     */
    java.util.Map<String, Object> financeStats();

    /**
     * 商家财务图表数据
     */
    java.util.Map<String, Object> financeChart();
}
