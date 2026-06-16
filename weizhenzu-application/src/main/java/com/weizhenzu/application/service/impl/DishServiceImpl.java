package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weizhenzu.application.service.DishService;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.domain.dto.DishDTO;
import com.weizhenzu.domain.entity.Dish;
import com.weizhenzu.domain.entity.DishSpec;
import com.weizhenzu.domain.vo.DishSpecVO;
import com.weizhenzu.domain.vo.DishVO;
import com.weizhenzu.infrastructure.persistence.mapper.DishMapper;
import com.weizhenzu.infrastructure.persistence.mapper.DishSpecMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜品服务实现
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishMapper dishMapper;
    private final DishSpecMapper dishSpecMapper;

    @Override
    public DishVO detail(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BizException(ResultCode.DISH_NOT_FOUND);
        }
        return toVO(dish, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(DishDTO dto) {
        Long merchantId = UserContext.getUserId();
        Dish dish = new Dish();
        dish.setMerchantId(merchantId);
        dish.setCategoryId(dto.getCategoryId());
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setImage(dto.getImage());
        dish.setImages(dto.getImages());
        dish.setPrice(dto.getPrice());
        dish.setOriginalPrice(dto.getOriginalPrice());
        dish.setStock(dto.getStock() == null ? -1 : dto.getStock());
        dish.setMonthSales(0);
        dish.setTotalSales(0);
        dish.setTags(dto.getTags());
        dish.setSpicy(dto.getSpicy());
        dish.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        dish.setSort(dto.getSort() == null ? 0 : dto.getSort());
        dishMapper.insert(dish);
        return dish.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DishDTO dto) {
        Dish dish = getAndCheckOwner(id);
        dish.setCategoryId(dto.getCategoryId());
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setImage(dto.getImage());
        dish.setImages(dto.getImages());
        dish.setPrice(dto.getPrice());
        dish.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getStock() != null) dish.setStock(dto.getStock());
        dish.setTags(dto.getTags());
        dish.setSpicy(dto.getSpicy());
        if (dto.getStatus() != null) dish.setStatus(dto.getStatus());
        if (dto.getSort() != null) dish.setSort(dto.getSort());
        dishMapper.updateById(dish);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Dish dish = getAndCheckOwner(id);
        dish.setStatus(status);
        dishMapper.updateById(dish);
    }

    @Override
    public void delete(Long id) {
        getAndCheckOwner(id);
        dishMapper.deleteById(id);
    }

    @Override
    public PageResult<DishVO> merchantPage(Integer current, Integer size, Long categoryId, String keyword) {
        Long merchantId = UserContext.getUserId();
        Page<Dish> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getMerchantId, merchantId)
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .like(keyword != null && !keyword.isEmpty(), Dish::getName, keyword)
                .orderByAsc(Dish::getSort)
                .orderByDesc(Dish::getCreatedAt);
        Page<Dish> result = dishMapper.selectPage(page, wrapper);
        List<DishVO> records = result.getRecords().stream()
                .map(d -> toVO(d, false))
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Dish getAndCheckOwner(Long id) {
        Long merchantId = UserContext.getUserId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !merchantId.equals(dish.getMerchantId())) {
            throw new BizException(ResultCode.DISH_NOT_FOUND);
        }
        return dish;
    }

    private DishVO toVO(Dish dish, boolean withSpecs) {
        DishVO vo = new DishVO();
        vo.setId(dish.getId());
        vo.setMerchantId(dish.getMerchantId());
        vo.setCategoryId(dish.getCategoryId());
        vo.setName(dish.getName());
        vo.setDescription(dish.getDescription());
        vo.setImage(dish.getImage());
        vo.setPrice(dish.getPrice());
        vo.setOriginalPrice(dish.getOriginalPrice());
        vo.setStock(dish.getStock());
        vo.setMonthSales(dish.getMonthSales());
        vo.setTotalSales(dish.getTotalSales());
        vo.setRating(dish.getRating());
        vo.setSpicy(dish.getSpicy());
        vo.setStatus(dish.getStatus());
        vo.setSort(dish.getSort());
        // tags/images 简化为 null（实际可解析 JSON）
        if (withSpecs) {
            List<DishSpec> specs = dishSpecMapper.selectList(
                    new LambdaQueryWrapper<DishSpec>()
                            .eq(DishSpec::getDishId, dish.getId())
                            .eq(DishSpec::getStatus, 1)
                            .orderByAsc(DishSpec::getSort));
            if (!CollectionUtils.isEmpty(specs)) {
                vo.setSpecs(specs.stream().map(s -> {
                    DishSpecVO sv = new DishSpecVO();
                    sv.setId(s.getId());
                    sv.setDishId(s.getDishId());
                    sv.setName(s.getName());
                    sv.setPriceDiff(s.getPriceDiff());
                    sv.setStock(s.getStock());
                    sv.setStatus(s.getStatus());
                    sv.setSort(s.getSort());
                    return sv;
                }).collect(Collectors.toList()));
            } else {
                vo.setSpecs(Collections.emptyList());
            }
        }
        return vo;
    }
}
