package com.weizhenzu.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weizhenzu.domain.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 站内消息 Mapper
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 标记已读
     */
    @Update("UPDATE t_message SET is_read = 1 WHERE user_id = #{userId} AND user_type = #{userType} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId, @Param("userType") Integer userType);
}
