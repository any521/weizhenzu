package com.weizhenzu.common.constant;

/**
 * 公共常量
 *
 * @author weizhenzu
 * @since 1.0.0
 */
public interface CommonConstants {

    String AUTH_HEADER = "Authorization";
    String TOKEN_PREFIX = "Bearer ";
    String USER_ID_HEADER = "X-User-Id";
    String USER_TYPE_HEADER = "X-User-Type";
    String TRACE_ID_HEADER = "X-Trace-Id";

    String REDIS_KEY_PREFIX = "weizhenzu:";
    String TOKEN_KEY = REDIS_KEY_PREFIX + "token:";
    String REFRESH_TOKEN_KEY = REDIS_KEY_PREFIX + "refresh:";
    String SMS_CODE_KEY = REDIS_KEY_PREFIX + "sms:";
    String SMS_CODE_INTERVAL_KEY = REDIS_KEY_PREFIX + "sms:interval:";
    String IDEMPOTENT_KEY = REDIS_KEY_PREFIX + "idempotent:";
    String LOCK_KEY = REDIS_KEY_PREFIX + "lock:";
    String RATE_LIMIT_KEY = REDIS_KEY_PREFIX + "rate:";
    String STOCK_KEY = REDIS_KEY_PREFIX + "stock:";

    long TOKEN_EXPIRE_SECONDS = 7200L;            // 2小时
    long REFRESH_TOKEN_EXPIRE_SECONDS = 604800L;  // 7天
    long SMS_CODE_EXPIRE_SECONDS = 300L;          // 5分钟
    long SMS_CODE_INTERVAL_SECONDS = 60L;         // 60秒间隔

    int MAX_PAGE_SIZE = 100;
    int DEFAULT_PAGE_SIZE = 10;
}
