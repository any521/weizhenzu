package com.weizhenzu.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权限类型
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
public enum PermissionType {

    MENU(1, "菜单"),
    BUTTON(2, "按钮"),
    API(3, "接口");

    private final Integer code;
    private final String desc;

    PermissionType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PermissionType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PermissionType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
