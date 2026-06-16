package com.weizhenzu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应 VO
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Schema(description = "登录响应")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "用户类型：1用户 2商家 3骑手 4管理员")
    private Integer userType;
}
