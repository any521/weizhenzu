package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weizhenzu.application.service.DishService;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.PageResult;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.domain.dto.DishDTO;
import com.weizhenzu.domain.entity.Dish;
import com.weizhenzu.domain.entity.DishCategory;
import com.weizhenzu.domain.entity.DishSpec;
import com.weizhenzu.domain.entity.Merchant;
import com.weizhenzu.domain.entity.MerchantCategory;
import com.weizhenzu.domain.vo.DishSpecVO;
import com.weizhenzu.domain.vo.DishVO;
import com.weizhenzu.infrastructure.persistence.mapper.DishCategoryMapper;
import com.weizhenzu.infrastructure.persistence.mapper.DishMapper;
import com.weizhenzu.infrastructure.persistence.mapper.DishSpecMapper;
import com.weizhenzu.infrastructure.persistence.mapper.MerchantCategoryMapper;
import com.weizhenzu.infrastructure.persistence.mapper.MerchantMapper;
import com.weizhenzu.infrastructure.thirdparty.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜品服务实现
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishMapper dishMapper;
    private final DishSpecMapper dishSpecMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final MerchantCategoryMapper merchantCategoryMapper;
    private final MerchantMapper merchantMapper;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

    @Override
    public DishVO detail(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !Integer.valueOf(1).equals(dish.getStatus())) {
            throw new BizException(ResultCode.DISH_NOT_FOUND);
        }
        DishVO vo = toVO(dish, true);
        fillCategoryNames(vo, dish);
        fillDishMerchantInfo(vo, dish);
        return vo;
    }

    @Override
    public DishVO merchantDetail(Long id) {
        Dish dish = getAndCheckOwner(id);
        DishVO vo = toVO(dish, true);
        fillCategoryNames(vo, dish);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(DishDTO dto) {
        Long merchantId = UserContext.getUserId();
        Dish dish = new Dish();
        dish.setMerchantId(merchantId);
        dish.setCategoryId(dto.getCategoryId());
        dish.setPlatformCategoryId(dto.getPlatformCategoryId());
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
        dish.setPlatformCategoryId(dto.getPlatformCategoryId());
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
        Map<Long, String> categoryNameMap = buildDishCategoryNameMap(result.getRecords());
        Map<Long, String> platformCategoryNameMap = buildPlatformCategoryNameMap(result.getRecords());
        List<DishVO> records = result.getRecords().stream()
                .map(d -> {
                    DishVO vo = toVO(d, false);
                    vo.setCategoryName(categoryNameMap.getOrDefault(d.getCategoryId(), ""));
                    vo.setPlatformCategoryName(platformCategoryNameMap.getOrDefault(d.getPlatformCategoryId(), ""));
                    return vo;
                })
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<DishVO> userPage(Integer current, Integer size, Long platformCategoryId, String keyword) {
        Page<Dish> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .eq(platformCategoryId != null, Dish::getPlatformCategoryId, platformCategoryId)
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(Dish::getName, keyword)
                        .or().like(Dish::getDescription, keyword))
                .orderByDesc(Dish::getMonthSales)
                .orderByDesc(Dish::getCreatedAt);
        Page<Dish> result = dishMapper.selectPage(page, wrapper);
        List<DishVO> records = result.getRecords().stream()
                .map(d -> {
                    DishVO vo = toVO(d, false);
                    fillDishMerchantInfo(vo, d);
                    fillCategoryNames(vo, d);
                    return vo;
                })
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<DishVO> featuredDishes(Integer limit, Integer diningType) {
        int lim = (limit == null || limit <= 0) ? 5 : limit;
        // 为了按商家配送类型筛选，查询更多菜品（最多取 lim*10），过滤后再截取
        int fetchLimit = (diningType != null) ? Math.max(lim * 10, 50) : lim;
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .orderByDesc(Dish::getCreatedAt)
                .last("LIMIT " + fetchLimit);
        List<Dish> dishes = dishMapper.selectList(wrapper);

        // 如果指定了 diningType，按商家的 supportDelivery/supportPickup 筛选
        if (diningType != null) {
            // 收集所有商家ID
            List<Long> merchantIds = dishes.stream()
                    .map(Dish::getMerchantId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            if (!merchantIds.isEmpty()) {
                // 批量查询商家
                List<Merchant> merchants = merchantMapper.selectBatchIds(merchantIds);
                // 构建 商家ID -> 是否支持当前配送类型 的映射
                Map<Long, Boolean> merchantSupportMap = merchants.stream()
                        .collect(Collectors.toMap(
                                Merchant::getId,
                                m -> {
                                    if (diningType == 2) {
                                        return Integer.valueOf(1).equals(m.getSupportDelivery());
                                    } else if (diningType == 3) {
                                        return Integer.valueOf(1).equals(m.getSupportPickup());
                                    }
                                    return true;
                                },
                                (a, b) -> a
                        ));
                // 过滤菜品：只保留所属商家支持当前配送类型的菜品
                dishes = dishes.stream()
                        .filter(d -> d.getMerchantId() == null
                                || merchantSupportMap.getOrDefault(d.getMerchantId(), false))
                        .limit(lim)
                        .collect(Collectors.toList());
            }
        }

        return dishes.stream()
                .map(d -> {
                    DishVO vo = toVO(d, false);
                    fillDishMerchantInfo(vo, d);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 填充菜品所属商家信息
     */
    private void fillDishMerchantInfo(DishVO vo, Dish dish) {
        if (dish.getMerchantId() != null) {
            Merchant m = merchantMapper.selectById(dish.getMerchantId());
            if (m != null) {
                vo.setMerchantName(m.getName());
                vo.setMerchantLogo(resolveImageUrl(m.getLogo()));
            }
        }
    }

    /**
     * 填充菜品的分类名称（商家分类和平台分类）
     */
    private void fillCategoryNames(DishVO vo, Dish dish) {
        if (dish.getCategoryId() != null) {
            DishCategory cat = dishCategoryMapper.selectById(dish.getCategoryId());
            if (cat != null) vo.setCategoryName(cat.getName());
        }
        if (dish.getPlatformCategoryId() != null) {
            MerchantCategory pCat = merchantCategoryMapper.selectById(dish.getPlatformCategoryId());
            if (pCat != null) vo.setPlatformCategoryName(pCat.getName());
        }
    }

    private Map<Long, String> buildDishCategoryNameMap(List<Dish> dishes) {
        List<Long> categoryIds = dishes.stream()
                .map(Dish::getCategoryId)
                .distinct()
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return dishCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(DishCategory::getId, DishCategory::getName, (a, b) -> a));
    }

    private Map<Long, String> buildPlatformCategoryNameMap(List<Dish> dishes) {
        List<Long> platformCategoryIds = dishes.stream()
                .map(Dish::getPlatformCategoryId)
                .distinct()
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (platformCategoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return merchantCategoryMapper.selectBatchIds(platformCategoryIds).stream()
                .collect(Collectors.toMap(MerchantCategory::getId, MerchantCategory::getName, (a, b) -> a));
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
        vo.setPlatformCategoryId(dish.getPlatformCategoryId());
        vo.setName(dish.getName());
        vo.setDescription(dish.getDescription());
        vo.setImage(resolveImageUrl(dish.getImage()));
        vo.setPrice(dish.getPrice());
        vo.setOriginalPrice(dish.getOriginalPrice());
        vo.setStock(dish.getStock());
        vo.setMonthSales(dish.getMonthSales());
        vo.setTotalSales(dish.getTotalSales());
        vo.setRating(dish.getRating());
        vo.setSpicy(dish.getSpicy());
        vo.setStatus(dish.getStatus());
        vo.setSort(dish.getSort());
        vo.setCreateTime(dish.getCreatedAt());
        // 解析 tags/images JSON
        vo.setTags(parseJsonList(dish.getTags()));
        List<String> images = parseJsonList(dish.getImages());
        vo.setImages(images.stream().map(this::resolveImageUrl).collect(Collectors.toList()));
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

    /**
     * 解析 JSON 字符串为 List
     */
    private List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("[Dish] 解析 JSON 失败: json={}, error={}", json, e.getMessage());
            // 兼容旧的逗号分隔格式
            if (json.contains(",")) {
                return List.of(json.split(","));
            }
            return List.of(json);
        }
    }

    private String resolveImageUrl(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        String trimmed = key.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
        return storageService.getAccessUrl(trimmed);
    }
}
