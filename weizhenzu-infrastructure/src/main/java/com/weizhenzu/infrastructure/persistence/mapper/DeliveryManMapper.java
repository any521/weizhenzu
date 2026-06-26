package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.DeliveryMan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 骑手 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface DeliveryManMapper extends BaseMapper<DeliveryMan> {

    /**
     * 根据手机号哈希查询骑手
     */
    @Select("SELECT * FROM t_delivery_man WHERE phone_hash = #{phoneHash} AND deleted = 0 LIMIT 1")
    DeliveryMan selectByPhoneHash(@Param("phoneHash") String phoneHash);

    /**
     * 根据邮箱查询骑手
     */
    @Select("SELECT * FROM t_delivery_man WHERE email = #{email} AND deleted = 0 LIMIT 1")
    DeliveryMan selectByEmail(@Param("email") String email);

    /**
     * 更新骑手位置
     */
    @Update("UPDATE t_delivery_man SET longitude = #{lng}, latitude = #{lat}, " +
            "location_time = NOW(), updated_at = NOW() WHERE id = #{id}")
    int updateLocation(@Param("id") Long id,
                       @Param("lng") java.math.BigDecimal lng,
                       @Param("lat") java.math.BigDecimal lat);
}
