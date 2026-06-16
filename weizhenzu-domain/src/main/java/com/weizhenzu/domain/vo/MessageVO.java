package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "站内消息")
public class MessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "类型：1订单 2优惠 3系统 4客服")
    private Integer type;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private Long bizId;

    @Schema(description = "是否已读")
    private Integer isRead;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
