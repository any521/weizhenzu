package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weizhenzu.application.service.CartService;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.domain.dto.CartAddDTO;
import com.weizhenzu.domain.dto.CartUpdateDTO;
import com.weizhenzu.domain.entity.Cart;
import com.weizhenzu.domain.entity.Dish;
import com.weizhenzu.domain.entity.DishSpec;
import com.weizhenzu.domain.entity.Merchant;
import com.weizhenzu.domain.vo.CartItemVO;
import com.weizhenzu.domain.vo.CartVO;
import com.weizhenzu.infrastructure.persistence.mapper.CartMapper;
import com.weizhenzu.infrastructure.persistence.mapper.DishMapper;
import com.weizhenzu.infrastructure.persistence.mapper.DishSpecMapper;
import com.weizhenzu.infrastructure.persistence.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final DishMapper dishMapper;
    private final DishSpecMapper dishSpecMapper;
    private final MerchantMapper merchantMapper;

    @Override
    public CartVO getCart() {
        Long userId = UserContext.getUserId();
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        if (carts.isEmpty()) {
            CartVO vo = new CartVO();
            vo.setTotalAmount(BigDecimal.ZERO);
            vo.setDeliveryFee(BigDecimal.ZERO);
            vo.setPackingFee(BigDecimal.ZERO);
            vo.setPayAmount(BigDecimal.ZERO);
            vo.setReachMinAmount(false);
            return vo;
        }

        Long merchantId = carts.get(0).getMerchantId();
        Merchant m = merchantMapper.selectById(merchantId);
        BigDecimal total = BigDecimal.ZERO;
        for (Cart c : carts) {
            total = total.add(c.getUnitPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
        }
        BigDecimal deliveryFee = m.getDeliveryFee() == null ? BigDecimal.ZERO : m.getDeliveryFee();
        BigDecimal packingFee = m.getPackingFee() == null ? BigDecimal.ZERO : m.getPackingFee();
        BigDecimal payAmount = total.add(deliveryFee).add(packingFee);
        BigDecimal minOrder = m.getMinOrderAmount() == null ? BigDecimal.ZERO : m.getMinOrderAmount();

        CartVO vo = new CartVO();
        vo.setMerchantId(merchantId);
        vo.setMerchantName(m.getName());
        vo.setItems(carts.stream().map(this::toItemVO).collect(Collectors.toList()));
        vo.setTotalAmount(total);
        vo.setDeliveryFee(deliveryFee);
        vo.setPackingFee(packingFee);
        vo.setPayAmount(payAmount);
        vo.setMinOrderAmount(minOrder);
        vo.setReachMinAmount(total.compareTo(minOrder) >= 0);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CartAddDTO dto) {
        Long userId = UserContext.getUserId();
        Dish dish = dishMapper.selectById(dto.getDishId());
        if (dish == null || dish.getStatus() != 1) {
            throw new BizException(ResultCode.DISH_NOT_FOUND);
        }
        if (!dto.getMerchantId().equals(dish.getMerchantId())) {
            throw new BizException(ResultCode.PARAM_ERROR, "菜品不属于该商家");
        }

        // 同一商家限制：清空其他商家的购物车
        cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .ne(Cart::getMerchantId, dto.getMerchantId()));

        BigDecimal unitPrice = dish.getPrice();
        String specName = null;
        if (dto.getSpecId() != null) {
            DishSpec spec = dishSpecMapper.selectById(dto.getSpecId());
            if (spec == null || spec.getStatus() != 1) {
                throw new BizException(ResultCode.PARAM_ERROR, "规格不存在");
            }
            unitPrice = dish.getPrice().add(spec.getPriceDiff());
            specName = spec.getName();
        }

        // 已存在则累加
        Cart exist = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getDishId, dto.getDishId())
                .eq(dto.getSpecId() != null, Cart::getSpecId, dto.getSpecId())
                .last("LIMIT 1"));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + dto.getQuantity());
            cartMapper.updateById(exist);
            return;
        }

        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setMerchantId(dto.getMerchantId());
        cart.setDishId(dto.getDishId());
        cart.setDishName(dish.getName());
        cart.setDishImage(dish.getImage());
        cart.setSpecId(dto.getSpecId());
        cart.setSpecName(specName);
        cart.setUnitPrice(unitPrice);
        cart.setQuantity(dto.getQuantity());
        cartMapper.insert(cart);
    }

    @Override
    public void update(Long id, CartUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        Cart cart = cartMapper.selectById(id);
        if (cart == null || !userId.equals(cart.getUserId())) {
            throw new BizException(ResultCode.NOT_FOUND, "购物车项不存在");
        }
        if (dto.getQuantity() <= 0) {
            cartMapper.deleteById(id);
        } else {
            cart.setQuantity(dto.getQuantity());
            cartMapper.updateById(cart);
        }
    }

    @Override
    public void delete(Long id) {
        Long userId = UserContext.getUserId();
        Cart cart = cartMapper.selectById(id);
        if (cart == null || !userId.equals(cart.getUserId())) {
            throw new BizException(ResultCode.NOT_FOUND, "购物车项不存在");
        }
        cartMapper.deleteById(id);
    }

    @Override
    public void clear() {
        Long userId = UserContext.getUserId();
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    private CartItemVO toItemVO(Cart c) {
        CartItemVO vo = new CartItemVO();
        vo.setId(c.getId());
        vo.setMerchantId(c.getMerchantId());
        vo.setDishId(c.getDishId());
        vo.setDishName(c.getDishName());
        vo.setDishImage(c.getDishImage());
        vo.setSpecId(c.getSpecId());
        vo.setSpecName(c.getSpecName());
        vo.setUnitPrice(c.getUnitPrice());
        vo.setQuantity(c.getQuantity());
        vo.setSubtotal(c.getUnitPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
        return vo;
    }
}
