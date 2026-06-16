package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weizhenzu.application.service.MerchantService;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.common.utils.PhoneUtils;
import com.weizhenzu.domain.dto.MerchantRegisterDTO;
import com.weizhenzu.domain.entity.Dish;
import com.weizhenzu.domain.entity.DishCategory;
import com.weizhenzu.domain.entity.Merchant;
import com.weizhenzu.domain.entity.MerchantCategory;
import com.weizhenzu.domain.vo.DishCategoryVO;
import com.weizhenzu.domain.vo.DishVO;
import com.weizhenzu.domain.vo.MerchantCategoryVO;
import com.weizhenzu.domain.vo.MerchantVO;
import com.weizhenzu.infrastructure.persistence.mapper.DishCategoryMapper;
import com.weizhenzu.infrastructure.persistence.mapper.DishMapper;
import com.weizhenzu.infrastructure.persistence.mapper.MerchantCategoryMapper;
import com.weizhenzu.infrastructure.persistence.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商家服务实现
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final DishMapper dishMapper;
    private final MerchantCategoryMapper merchantCategoryMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Long register(MerchantRegisterDTO dto) {
        String phoneHash = PhoneUtils.hash(dto.getPhone());
        Merchant exists = merchantMapper.selectByPhoneHash(phoneHash);
        if (exists != null) {
            throw new BizException(ResultCode.PARAM_ERROR, "手机号已注册");
        }
        Merchant m = new Merchant();
        m.setPhone(dto.getPhone());
        m.setPhoneHash(phoneHash);
        m.setPassword(passwordEncoder.encode(dto.getPassword()));
        m.setName(dto.getName());
        m.setCategoryId(dto.getCategoryId());
        m.setContactName(dto.getContactName());
        m.setContactPhone(dto.getContactPhone());
        m.setProvince(dto.getProvince());
        m.setCity(dto.getCity());
        m.setDistrict(dto.getDistrict());
        m.setAddress(dto.getAddress());
        m.setLongitude(dto.getLongitude());
        m.setLatitude(dto.getLatitude());
        m.setDescription(dto.getDescription());
        m.setNotice(dto.getNotice());
        m.setIsOpen(1);
        m.setStatus(0);
        m.setRating(new java.math.BigDecimal("5.0"));
        m.setMonthSales(0);
        merchantMapper.insert(m);
        return m.getId();
    }

    @Override
    public MerchantVO detail(Long id) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new BizException(ResultCode.MERCHANT_NOT_FOUND);
        }
        return toVO(m);
    }

    @Override
    public MerchantVO current() {
        Long id = UserContext.getUserId();
        return detail(id);
    }

    @Override
    public PageResult<MerchantVO> userPage(Integer current, Integer size, Long categoryId, String keyword) {
        Page<Merchant> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getStatus, 1)
                .eq(Merchant::getIsOpen, 1)
                .eq(categoryId != null, Merchant::getCategoryId, categoryId)
                .like(keyword != null && !keyword.isEmpty(), Merchant::getName, keyword)
                .orderByDesc(Merchant::getRating);
        Page<Merchant> result = merchantMapper.selectPage(page, wrapper);
        List<MerchantVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<DishCategoryVO> menu(Long merchantId) {
        Merchant m = merchantMapper.selectById(merchantId);
        if (m == null) {
            throw new BizException(ResultCode.MERCHANT_NOT_FOUND);
        }
        List<DishCategory> categories = dishCategoryMapper.selectList(
                new LambdaQueryWrapper<DishCategory>()
                        .eq(DishCategory::getMerchantId, merchantId)
                        .eq(DishCategory::getStatus, 1)
                        .orderByAsc(DishCategory::getSort));
        return categories.stream().map(c -> {
            DishCategoryVO vo = new DishCategoryVO();
            vo.setId(c.getId());
            vo.setMerchantId(c.getMerchantId());
            vo.setName(c.getName());
            vo.setSort(c.getSort());
            vo.setStatus(c.getStatus());
            List<Dish> dishes = dishMapper.selectList(
                    new LambdaQueryWrapper<Dish>()
                            .eq(Dish::getCategoryId, c.getId())
                            .eq(Dish::getStatus, 1)
                            .orderByAsc(Dish::getSort));
            vo.setDishes(dishes.stream().map(d -> {
                DishVO dv = new DishVO();
                dv.setId(d.getId());
                dv.setMerchantId(d.getMerchantId());
                dv.setCategoryId(d.getCategoryId());
                dv.setName(d.getName());
                dv.setDescription(d.getDescription());
                dv.setImage(d.getImage());
                dv.setPrice(d.getPrice());
                dv.setOriginalPrice(d.getOriginalPrice());
                dv.setStock(d.getStock());
                dv.setMonthSales(d.getMonthSales());
                dv.setRating(d.getRating());
                dv.setSpicy(d.getSpicy());
                dv.setStatus(d.getStatus());
                dv.setSort(d.getSort());
                dv.setSpecs(Collections.emptyList());
                return dv;
            }).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<MerchantCategoryVO> categories() {
        List<MerchantCategory> list = merchantCategoryMapper.selectList(
                new LambdaQueryWrapper<MerchantCategory>()
                        .eq(MerchantCategory::getStatus, 1)
                        .orderByAsc(MerchantCategory::getSort));
        return list.stream().map(c -> {
            MerchantCategoryVO vo = new MerchantCategoryVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setIcon(c.getIcon());
            return vo;
        }).collect(Collectors.toList());
    }

    private MerchantVO toVO(Merchant m) {
        MerchantVO vo = new MerchantVO();
        vo.setId(m.getId());
        vo.setName(m.getName());
        vo.setLogo(m.getLogo());
        vo.setCategoryId(m.getCategoryId());
        vo.setDescription(m.getDescription());
        vo.setNotice(m.getNotice());
        vo.setProvince(m.getProvince());
        vo.setCity(m.getCity());
        vo.setDistrict(m.getDistrict());
        vo.setAddress(m.getAddress());
        vo.setLongitude(m.getLongitude());
        vo.setLatitude(m.getLatitude());
        vo.setDeliveryRadius(m.getDeliveryRadius());
        vo.setMinOrderAmount(m.getMinOrderAmount());
        vo.setDeliveryFee(m.getDeliveryFee());
        vo.setPackingFee(m.getPackingFee());
        vo.setOpenTime(m.getOpenTime());
        vo.setIsOpen(m.getIsOpen());
        vo.setStatus(m.getStatus());
        vo.setRating(m.getRating());
        vo.setMonthSales(m.getMonthSales());
        return vo;
    }
}
