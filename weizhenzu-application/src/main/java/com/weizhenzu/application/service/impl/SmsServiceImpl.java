package com.weizhenzu.application.service.impl;

import com.weizhenzu.application.service.SmsService;
import com.weizhenzu.common.constant.CommonConstants;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import com.weizhenzu.domain.entity.SmsLog;
import com.weizhenzu.infrastructure.persistence.mapper.SmsLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 短信服务实现（毕业设计模拟，验证码固定 1234）
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final StringRedisTemplate redis;
    private final SmsLogMapper smsLogMapper;

    @Override
    public void sendCode(String phone, String scene) {
        // 1. 频率限制：60 秒内只能发一次
        String intervalKey = CommonConstants.SMS_CODE_INTERVAL_KEY + phone + ":" + scene;
        Boolean ok = redis.opsForValue().setIfAbsent(intervalKey, "1",
                Duration.ofSeconds(CommonConstants.SMS_CODE_INTERVAL_SECONDS));
        if (Boolean.FALSE.equals(ok)) {
            throw new BizException(ResultCode.SMS_CODE_FREQUENT);
        }

        // 2. 生成验证码（毕业设计固定 1234，便于测试）
        String code = "1234";

        // 3. 存入 Redis，5 分钟过期
        String codeKey = CommonConstants.SMS_CODE_KEY + phone + ":" + scene;
        redis.opsForValue().set(codeKey, code,
                Duration.ofSeconds(CommonConstants.SMS_CODE_EXPIRE_SECONDS));

        // 4. 记录日志
        SmsLog smsLog = new SmsLog();
        smsLog.setPhone(phone);
        smsLog.setCode(code);
        smsLog.setScene(scene);
        smsLog.setContent("【味真足】您的验证码是 " + code + "，5分钟内有效");
        smsLog.setChannel("MOCK");
        smsLog.setStatus(1);
        smsLog.setExpireTime(LocalDateTime.now().plusSeconds(CommonConstants.SMS_CODE_EXPIRE_SECONDS));
        smsLogMapper.insert(smsLog);

        log.info("[短信验证码] phone={}, scene={}, code={}", phone, scene, code);
    }

    @Override
    public boolean verifyCode(String phone, String code, String scene) {
        String codeKey = CommonConstants.SMS_CODE_KEY + phone + ":" + scene;
        String cached = redis.opsForValue().get(codeKey);
        if (cached == null || !cached.equals(code)) {
            return false;
        }
        // 验证后删除
        redis.delete(codeKey);
        return true;
    }
}
