package com.weizhenzu.application.service;

import com.weizhenzu.domain.dto.CartAddDTO;
import com.weizhenzu.domain.dto.CartUpdateDTO;
import com.weizhenzu.domain.vo.CartVO;

/**
 * 购物车服务接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface CartService {

    /**
     * 获取购物车
     */
    CartVO getCart();

    /**
     * 加入购物车
     */
    void add(CartAddDTO dto);

    /**
     * 修改购物车数量
     */
    void update(Long id, CartUpdateDTO dto);

    /**
     * 删除购物车项
     */
    void delete(Long id);

    /**
     * 清空购物车
     */
    void clear();

    /**
     * 清空指定商家的购物车（下单成功后调用）
     */
    void clearByMerchant(Long userId, Long merchantId);
}
