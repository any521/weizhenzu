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
import com.weizhenzu.infrastructure.persistence.mapper.OrderMapper;
import com.weizhenzu.infrastructure.thirdparty.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
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
    private final OrderMapper orderMapper;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;

    /** 地球半径（公里） */
    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public Long register(MerchantRegisterDTO dto) {
        String phoneHash = PhoneUtils.hash(dto.getPhone());
        Merchant exists = merchantMapper.selectByPhoneHashRaw(phoneHash);
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
    public MerchantVO detail(Long id, BigDecimal lng, BigDecimal lat) {
        Merchant m = merchantMapper.selectByIdRaw(id);
        if (m == null) {
            throw new BizException(ResultCode.MERCHANT_NOT_FOUND);
        }
        MerchantVO vo = toVO(m);
        // 如果传入了用户坐标，计算距离
        if (lng != null && lat != null && m.getLongitude() != null && m.getLatitude() != null) {
            int distance = calculateDistance(
                    lng.doubleValue(), lat.doubleValue(),
                    m.getLongitude().doubleValue(), m.getLatitude().doubleValue());
            vo.setDistance(distance);
        }
        return vo;
    }

    @Override
    public MerchantVO current() {
        Long id = UserContext.getUserId();
        return detail(id, null, null);
    }

    @Override
    public PageResult<MerchantVO> userPage(Integer current, Integer size, Long categoryId, String keyword,
                                          Integer deliveryType, BigDecimal lng, BigDecimal lat) {
        Page<Merchant> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getStatus, 1)
                .eq(Merchant::getIsOpen, 1)
                .eq(categoryId != null, Merchant::getCategoryId, categoryId)
                .like(keyword != null && !keyword.isEmpty(), Merchant::getName, keyword)
                .eq(deliveryType != null && deliveryType == 1, Merchant::getSupportDelivery, 1)
                .eq(deliveryType != null && deliveryType == 2, Merchant::getSupportPickup, 1);

        // 如果有用户坐标，查询所有商家后按距离排序（内存计算，适合小数据量）
        // 大数据量场景建议使用MySQL ST_Distance_Sphere或GeoHash
        List<Merchant> merchantList;
        long total;
        if (lng != null && lat != null) {
            // 不分页查询所有符合条件的商家（用于距离排序），实际生产环境应使用地理空间查询
            wrapper.orderByDesc(Merchant::getRating);
            List<Merchant> all = merchantMapper.selectList(wrapper);
            total = all.size();
            // 计算距离并按距离排序
            List<Merchant> sorted = all.stream()
                    .sorted(Comparator.comparingInt(m -> {
                        if (m.getLongitude() == null || m.getLatitude() == null) return Integer.MAX_VALUE;
                        return calculateDistance(lng.doubleValue(), lat.doubleValue(),
                                m.getLongitude().doubleValue(), m.getLatitude().doubleValue());
                    }))
                    .collect(Collectors.toList());
            // 手动分页
            int fromIndex = (int) ((current == null ? 1 : current) - 1) * (size == null ? 10 : size);
            int toIndex = Math.min(fromIndex + (size == null ? 10 : size), sorted.size());
            if (fromIndex >= sorted.size()) {
                merchantList = Collections.emptyList();
            } else {
                merchantList = sorted.subList(fromIndex, toIndex);
            }
        } else {
            wrapper.orderByDesc(Merchant::getRating);
            Page<Merchant> result = merchantMapper.selectPage(page, wrapper);
            merchantList = result.getRecords();
            total = result.getTotal();
        }

        // 转换为VO并设置距离
        double userLng = lng != null ? lng.doubleValue() : 0;
        double userLat = lat != null ? lat.doubleValue() : 0;
        List<MerchantVO> records = merchantList.stream()
                .map(m -> {
                    MerchantVO vo = toVO(m);
                    if (lng != null && lat != null && m.getLongitude() != null && m.getLatitude() != null) {
                        vo.setDistance(calculateDistance(userLng, userLat,
                                m.getLongitude().doubleValue(), m.getLatitude().doubleValue()));
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        int cur = current == null ? 1 : current;
        int sz = size == null ? 10 : size;
        return PageResult.of(records, (long) total, (long) cur, (long) sz);
    }

    /**
     * Haversine公式计算两点间直线距离（米）
     */
    private int calculateDistance(double lng1, double lat1, double lng2, double lat2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = EARTH_RADIUS_KM * c;
        return (int) Math.round(distanceKm * 1000);
    }

    @Override
    public List<DishCategoryVO> menu(Long merchantId) {
        Merchant m = merchantMapper.selectByIdRaw(merchantId);
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
                dv.setPlatformCategoryId(d.getPlatformCategoryId());
                dv.setName(d.getName());
                dv.setDescription(d.getDescription());
                dv.setImage(resolveImageUrl(d.getImage()));
                dv.setPrice(d.getPrice());
                dv.setOriginalPrice(d.getOriginalPrice());
                dv.setStock(d.getStock());
                dv.setMonthSales(d.getMonthSales());
                dv.setRating(d.getRating());
                dv.setSpicy(d.getSpicy());
                dv.setStatus(d.getStatus());
                dv.setSort(d.getSort());
                dv.setCategoryName(c.getName());
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
            vo.setColor(c.getColor());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateSettings(String name, String logo, String contactPerson, String phone, String description, String notice,
                               String openTime, String address, Integer isOpen, BigDecimal minOrderAmount,
                               BigDecimal deliveryFee, BigDecimal packingFee, Integer deliveryRadius,
                               Integer supportDelivery, Integer supportPickup, BigDecimal longitude, BigDecimal latitude) {
        Long merchantId = UserContext.getUserId();
        Merchant m = merchantMapper.selectByIdRaw(merchantId);
        if (m == null) {
            throw new BizException(ResultCode.MERCHANT_NOT_FOUND);
        }
        Merchant update = new Merchant();
        update.setId(merchantId);
        if (name != null) update.setName(name);
        if (logo != null) update.setLogo(logo);
        if (contactPerson != null) update.setContactName(contactPerson);
        if (phone != null) update.setContactPhone(phone);
        if (description != null) update.setDescription(description);
        if (notice != null) update.setNotice(notice);
        if (openTime != null) update.setOpenTime(openTime);
        if (address != null) update.setAddress(address);
        if (isOpen != null) update.setIsOpen(isOpen);
        if (minOrderAmount != null) update.setMinOrderAmount(minOrderAmount);
        if (deliveryFee != null) update.setDeliveryFee(deliveryFee);
        if (packingFee != null) update.setPackingFee(packingFee);
        if (deliveryRadius != null) update.setDeliveryRadius(deliveryRadius);
        if (supportDelivery != null) update.setSupportDelivery(supportDelivery);
        if (supportPickup != null) update.setSupportPickup(supportPickup);
        if (longitude != null) update.setLongitude(longitude);
        if (latitude != null) update.setLatitude(latitude);
        merchantMapper.updateById(update);
    }

    @Override
    public java.util.Map<String, Object> financeStats() {
        Long merchantId = UserContext.getUserId();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        java.time.LocalDate today = java.time.LocalDate.now();

        // 今日成交
        java.util.List<com.weizhenzu.domain.entity.Order> todayOrders = orderMapperByMerchant(merchantId,
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal todayIncome = todayOrders.stream()
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("todayIncome", todayIncome);
        data.put("todayOrderCount", todayOrders.size());

        // 本周成交
        java.time.LocalDate weekStart = today.minusDays(6);
        java.util.List<com.weizhenzu.domain.entity.Order> weekOrders = orderMapperByMerchant(merchantId,
                weekStart.atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal weekIncome = weekOrders.stream()
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("weekIncome", weekIncome);

        // 本月成交
        java.time.LocalDate monthStart = today.minusDays(29);
        java.util.List<com.weizhenzu.domain.entity.Order> monthOrders = orderMapperByMerchant(merchantId,
                monthStart.atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal monthIncome = monthOrders.stream()
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("monthIncome", monthIncome);

        // 总成交
        java.util.List<com.weizhenzu.domain.entity.Order> allOrders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.weizhenzu.domain.entity.Order>()
                        .eq(com.weizhenzu.domain.entity.Order::getMerchantId, merchantId)
                        .eq(com.weizhenzu.domain.entity.Order::getPayStatus, 1));
        BigDecimal totalIncome = allOrders.stream()
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("totalIncome", totalIncome);

        // 退款金额
        data.put("refundAmount", BigDecimal.ZERO);
        return data;
    }

    @Override
    public java.util.Map<String, Object> financeChart() {
        Long merchantId = UserContext.getUserId();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        java.util.List<String> dates = new java.util.ArrayList<>();
        java.util.List<java.math.BigDecimal> values = new java.util.ArrayList<>();
        java.util.List<String> legend = new java.util.ArrayList<>();
        legend.add("营业额");

        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate date = today.minusDays(i);
            java.util.List<com.weizhenzu.domain.entity.Order> dayOrders = orderMapperByMerchant(merchantId,
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay());
            BigDecimal dayIncome = dayOrders.stream()
                    .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dates.add(date.toString());
            values.add(dayIncome);
        }

        data.put("dates", dates);
        data.put("values", values);
        data.put("legend", legend);
        java.util.List<java.util.Map<String, Object>> series = new java.util.ArrayList<>();
        java.util.Map<String, Object> s = new java.util.HashMap<>();
        s.put("name", "营业额");
        s.put("data", values);
        series.add(s);
        data.put("series", series);
        return data;
    }

    /**
     * 查询商家在指定时间范围内的已支付订单
     */
    private java.util.List<com.weizhenzu.domain.entity.Order> orderMapperByMerchant(Long merchantId,
                                                                                     java.time.LocalDateTime start,
                                                                                     java.time.LocalDateTime end) {
        return orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.weizhenzu.domain.entity.Order>()
                        .eq(com.weizhenzu.domain.entity.Order::getMerchantId, merchantId)
                        .eq(com.weizhenzu.domain.entity.Order::getPayStatus, 1)
                        .ge(com.weizhenzu.domain.entity.Order::getPayTime, start)
                        .lt(com.weizhenzu.domain.entity.Order::getPayTime, end));
    }

    private MerchantVO toVO(Merchant m) {
        MerchantVO vo = new MerchantVO();
        vo.setId(m.getId());
        vo.setName(m.getName());
        vo.setLogo(resolveImageUrl(m.getLogo()));
        vo.setCategoryId(m.getCategoryId());
        // 填充分类名称
        if (m.getCategoryId() != null) {
            MerchantCategory category = merchantCategoryMapper.selectById(m.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
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
        vo.setMonthlySales(m.getMonthSales());
        vo.setSupportDelivery(m.getSupportDelivery());
        vo.setSupportPickup(m.getSupportPickup());
        // 补充缺失的字段映射
        vo.setContactPerson(m.getContactName());
        vo.setPhone(m.getContactPhone());
        vo.setCreateTime(m.getCreatedAt());
        vo.setQualification(m.getQualification());
        return vo;
    }

    private String resolveImageUrl(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        String trimmed = key.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
        return storageService.getAccessUrl(trimmed);
    }
}
