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
import com.weizhenzu.domain.vo.CartGroupVO;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 购物车服务实现（支持多商家）
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
            vo.setTotalCount(0);
            vo.setGroups(Collections.emptyList());
            vo.setItems(Collections.emptyList());
            return vo;
        }

        // 按商家分组
        Map<Long, List<Cart>> grouped = carts.stream()
                .collect(Collectors.groupingBy(Cart::getMerchantId));

        List<CartGroupVO> groups = new ArrayList<>();
        BigDecimal allTotal = BigDecimal.ZERO;
        int allCount = 0;

        for (Map.Entry<Long, List<Cart>> entry : grouped.entrySet()) {
            Long mid = entry.getKey();
            List<Cart> items = entry.getValue();
            Merchant m = merchantMapper.selectById(mid);
            if (m == null) continue;

            BigDecimal groupTotal = BigDecimal.ZERO;
            List<CartItemVO> itemVOs = new ArrayList<>();
            for (Cart c : items) {
                groupTotal = groupTotal.add(c.getUnitPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
                itemVOs.add(toItemVO(c));
                allCount += c.getQuantity();
            }
            allTotal = allTotal.add(groupTotal);

            BigDecimal deliveryFee = m.getDeliveryFee() == null ? BigDecimal.ZERO : m.getDeliveryFee();
            BigDecimal packingFee = m.getPackingFee() == null ? BigDecimal.ZERO : m.getPackingFee();
            BigDecimal payAmount = groupTotal.add(deliveryFee).add(packingFee);
            BigDecimal minOrder = m.getMinOrderAmount() == null ? BigDecimal.ZERO : m.getMinOrderAmount();

            CartGroupVO g = new CartGroupVO();
            g.setMerchantId(mid);
            g.setMerchantName(m.getName());
            g.setMerchantLogo(m.getLogo());
            g.setItems(itemVOs);
            g.setTotalAmount(groupTotal);
            g.setDeliveryFee(deliveryFee);
            g.setPackingFee(packingFee);
            g.setPayAmount(payAmount);
            g.setMinOrderAmount(minOrder);
            g.setReachMinAmount(groupTotal.compareTo(minOrder) >= 0);
            groups.add(g);
        }

        // 按商家ID排序，保持一致性
        groups.sort(Comparator.comparing(CartGroupVO::getMerchantId));

        CartVO vo = new CartVO();
        vo.setGroups(groups);
        vo.setTotalAmount(allTotal);
        vo.setTotalCount(allCount);

        // 兼容旧前端：取第一个商家的数据
        if (!groups.isEmpty()) {
            CartGroupVO first = groups.get(0);
            vo.setMerchantId(first.getMerchantId());
            vo.setMerchantName(first.getMerchantName());
            vo.setItems(first.getItems());
            vo.setDeliveryFee(first.getDeliveryFee());
            vo.setPackingFee(first.getPackingFee());
            vo.setPayAmount(first.getPayAmount());
            vo.setMinOrderAmount(first.getMinOrderAmount());
            vo.setReachMinAmount(first.getReachMinAmount());
        } else {
            vo.setItems(Collections.emptyList());
            vo.setDeliveryFee(BigDecimal.ZERO);
            vo.setPackingFee(BigDecimal.ZERO);
            vo.setPayAmount(BigDecimal.ZERO);
            vo.setReachMinAmount(false);
        }

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

        // 多商家模式：不再清空其他商家的购物车

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

        // 已存在同菜品同规格则累加
        Cart exist = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getMerchantId, dto.getMerchantId())
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

    /**
     * 清空指定商家的购物车项（下单成功后调用）
     */
    public void clearByMerchant(Long userId, Long merchantId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getMerchantId, merchantId));
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
