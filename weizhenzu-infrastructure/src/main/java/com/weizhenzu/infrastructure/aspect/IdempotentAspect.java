package com.weizhenzu.infrastructure.aspect;

import com.weizhenzu.common.annotation.Idempotent;
import com.weizhenzu.common.constant.CommonConstants;
import com.weizhenzu.common.context.UserContext;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 幂等切面
 * <p>基于 Redis SETNX 实现接口幂等性</p>
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate redis;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer pnd = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        String keyValue = resolveKeyValue(pjp, idempotent);

        // 如果无法解析key值，直接放行（不拦截），避免误判
        if (!StringUtils.hasText(keyValue)) {
            log.warn("[Idempotent] 无法解析幂等key表达式 '{}', 直接放行", idempotent.key());
            return pjp.proceed();
        }

        Long uid = UserContext.getUserId();
        String key = CommonConstants.IDEMPOTENT_KEY
                + (uid == null ? "" : uid + ":") + keyValue;

        log.info("[Idempotent] 开始幂等检查, method={}, key={}", 
                pjp.getSignature().toShortString(), key);

        Boolean ok = redis.opsForValue().setIfAbsent(key, "1",
                Duration.of(idempotent.expire(), idempotent.timeUnit().toChronoUnit()));
        if (Boolean.FALSE.equals(ok)) {
            log.warn("[Idempotent] 重复请求被拦截, key={}", key);
            throw new BizException(ResultCode.IDEMPOTENT, idempotent.message());
        }
        
        log.info("[Idempotent] 幂等检查通过, key={}, 开始执行业务逻辑", key);
        
        try {
            Object result = pjp.proceed();
            log.info("[Idempotent] 业务执行成功, key={}", key);
            return result;
        } catch (Throwable e) {
            // 执行失败释放幂等锁，允许重试
            log.warn("[Idempotent] 业务执行异常, 释放幂等锁, key={}, error={}", key, e.getMessage());
            redis.delete(key);
            throw e;
        }
    }

    /**
     * 解析幂等key值，支持多种方式获取：
     * 1. 通过SpEL参数名解析（#dto.clientToken）
     * 2. 通过参数索引解析（#a0.clientToken, #p0.clientToken）
     * 3. 如果SpEL失败，通过反射从第一个参数中获取字段值
     */
    private String resolveKeyValue(ProceedingJoinPoint pjp, Idempotent idempotent) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Object[] args = pjp.getArgs();

        // 方式1：通过参数名解析
        try {
            String[] paramNames = pnd.getParameterNames(method);
            EvaluationContext ctx = new StandardEvaluationContext();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    ctx.setVariable(paramNames[i], args[i]);
                    ctx.setVariable("a" + i, args[i]);
                    ctx.setVariable("p" + i, args[i]);
                }
            }
            Expression exp = parser.parseExpression(idempotent.key());
            Object value = exp.getValue(ctx);
            if (value != null && StringUtils.hasText(value.toString())) {
                log.debug("[Idempotent] SpEL解析成功, key={}, value={}", idempotent.key(), value);
                return value.toString();
            }
        } catch (Exception e) {
            log.debug("[Idempotent] SpEL参数名解析失败: {}", e.getMessage());
        }

        // 方式2：反射兜底 - 从第一个参数中查找与key表达式最后一段同名的字段
        String keyExpr = idempotent.key();
        String fieldName = keyExpr;
        if (keyExpr.contains(".")) {
            fieldName = keyExpr.substring(keyExpr.lastIndexOf('.') + 1);
        }
        if (args.length > 0 && args[0] != null) {
            try {
                Object val = getFieldValue(args[0], fieldName);
                if (val != null && StringUtils.hasText(val.toString())) {
                    log.debug("[Idempotent] 反射获取字段成功, field={}, value={}", fieldName, val);
                    return val.toString();
                }
            } catch (Exception e) {
                log.debug("[Idempotent] 反射获取字段失败: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * 反射获取对象字段值（支持Lombok @Data生成的getter方法）
     */
    private Object getFieldValue(Object obj, String fieldName) {
        Class<?> clazz = obj.getClass();
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            Method getter = clazz.getMethod(getterName);
            return getter.invoke(obj);
        } catch (Exception ignored) {
        }
        // 尝试直接访问字段
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception ignored) {
        }
        return null;
    }
}
