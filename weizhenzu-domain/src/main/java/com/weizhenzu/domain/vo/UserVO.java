package com.weizhenzu.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * C端用户信息 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "用户信息")
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "手机号（脱敏）")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别：0未知 1男 2女")
    private Integer gender;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "会员等级")
    private Integer level;

    @Schema(description = "积分")
    private Integer points;

    @Schema(description = "余额")
    private BigDecimal balance;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
