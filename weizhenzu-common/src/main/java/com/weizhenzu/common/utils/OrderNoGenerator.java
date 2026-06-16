package com.weizhenzu.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务编号生成器
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public class OrderNoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderNoGenerator() {}

    public static String generate(String prefix) {
        return prefix + LocalDateTime.now().format(FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    public static String orderNo() {
        return generate("OD");
    }

    public static String paymentNo() {
        return generate("PAY");
    }

    public static String refundNo() {
        return generate("RF");
    }

    public static String taskNo() {
        return generate("DT");
    }

    public static String settleNo() {
        return generate("ST");
    }
}
