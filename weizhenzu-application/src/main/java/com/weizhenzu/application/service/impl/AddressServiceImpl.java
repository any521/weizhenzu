package com.weizhenzu.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weizhenzu.application.service.AddressService;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.common.utils.PhoneUtils;
import com.weizhenzu.domain.dto.AddressDTO;
import com.weizhenzu.domain.entity.Address;
import com.weizhenzu.domain.vo.AddressVO;
import com.weizhenzu.infrastructure.persistence.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 收货地址服务实现
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<AddressVO> list() {
        Long userId = UserContext.getUserId();
        List<Address> list = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getCreatedAt));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AddressVO detail(Long id) {
        Address addr = getAndCheckOwner(id);
        return toVO(addr);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(AddressDTO dto) {
        Long userId = UserContext.getUserId();
        Address addr = new Address();
        addr.setUserId(userId);
        addr.setContactName(dto.getContactName());
        addr.setContactPhone(dto.getContactPhone());
        addr.setProvince(dto.getProvince());
        addr.setCity(dto.getCity());
        addr.setDistrict(dto.getDistrict());
        addr.setDetail(dto.getDetail());
        addr.setLongitude(dto.getLongitude());
        addr.setLatitude(dto.getLatitude());
        addr.setTag(dto.getTag());
        addr.setIsDefault(dto.getIsDefault() == null ? 0 : dto.getIsDefault());

        if (Integer.valueOf(1).equals(addr.getIsDefault())) {
            addressMapper.clearDefault(userId);
        }
        addressMapper.insert(addr);
        return addr.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AddressDTO dto) {
        Address addr = getAndCheckOwner(id);
        addr.setContactName(dto.getContactName());
        addr.setContactPhone(dto.getContactPhone());
        addr.setProvince(dto.getProvince());
        addr.setCity(dto.getCity());
        addr.setDistrict(dto.getDistrict());
        addr.setDetail(dto.getDetail());
        addr.setLongitude(dto.getLongitude());
        addr.setLatitude(dto.getLatitude());
        addr.setTag(dto.getTag());
        if (Integer.valueOf(1).equals(dto.getIsDefault())) {
            addressMapper.clearDefault(addr.getUserId());
            addr.setIsDefault(1);
        }
        addressMapper.updateById(addr);
    }

    @Override
    public void delete(Long id) {
        Address addr = getAndCheckOwner(id);
        addressMapper.deleteById(addr.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Address addr = getAndCheckOwner(id);
        addressMapper.clearDefault(addr.getUserId());
        addr.setIsDefault(1);
        addressMapper.updateById(addr);
    }

    private Address getAndCheckOwner(Long id) {
        Long userId = UserContext.getUserId();
        Address addr = addressMapper.selectById(id);
        if (addr == null || !userId.equals(addr.getUserId())) {
            throw new BizException(ResultCode.NOT_FOUND, "地址不存在");
        }
        return addr;
    }

    private AddressVO toVO(Address addr) {
        AddressVO vo = new AddressVO();
        vo.setId(addr.getId());
        vo.setContactName(addr.getContactName());
        vo.setContactPhone(PhoneUtils.mask(addr.getContactPhone()));
        vo.setProvince(addr.getProvince());
        vo.setCity(addr.getCity());
        vo.setDistrict(addr.getDistrict());
        vo.setDetail(addr.getDetail());
        vo.setLongitude(addr.getLongitude());
        vo.setLatitude(addr.getLatitude());
        vo.setTag(addr.getTag());
        vo.setIsDefault(addr.getIsDefault());
        return vo;
    }
}
