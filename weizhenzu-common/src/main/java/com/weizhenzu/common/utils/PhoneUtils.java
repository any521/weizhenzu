package com.weizhenzu.common.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

/**
 * 手机号加密/脱敏工具
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public class PhoneUtils {

    private static final String AES_KEY = "weizhenzu-aes-key-256bit-32bytes";

    private PhoneUtils() {}

    public static String encrypt(String phone) {
        if (phone == null) return null;
        AES aes = new AES(AES_KEY.getBytes(StandardCharsets.UTF_8));
        return aes.encryptHex(phone);
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null) return null;
        AES aes = new AES(AES_KEY.getBytes(StandardCharsets.UTF_8));
        return aes.decryptStr(encrypted);
    }

    public static String hash(String phone) {
        if (phone == null) return null;
        return DigestUtil.sha256Hex("phone:" + phone);
    }

    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 校验是否为合法手机号
     */
    public static boolean isPhone(String str) {
        return str != null && str.matches("^1[3-9]\\d{9}$");
    }
}
