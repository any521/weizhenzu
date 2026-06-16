package com.weizhenzu.application.service;

import com.weizhenzu.domain.dto.AddressDTO;
import com.weizhenzu.domain.vo.AddressVO;

import java.util.List;

/**
 * 收货地址服务接口
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface AddressService {

    List<AddressVO> list();

    AddressVO detail(Long id);

    Long add(AddressDTO dto);

    void update(Long id, AddressDTO dto);

    void delete(Long id);

    void setDefault(Long id);
}
