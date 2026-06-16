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
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Object[] args = pjp.getArgs();
        String[] paramNames = pnd.getParameterNames(method);

        EvaluationContext ctx = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }
        Expression exp = parser.parseExpression(idempotent.key());
        Object value = exp.getValue(ctx);
        Long uid = UserContext.getUserId();
        String key = CommonConstants.IDEMPOTENT_KEY
                + (uid == null ? "" : uid + ":") + value;

        Boolean ok = redis.opsForValue().setIfAbsent(key, "1",
                Duration.of(idempotent.expire(), idempotent.timeUnit().toChronoUnit()));
        if (Boolean.FALSE.equals(ok)) {
            throw new BizException(ResultCode.IDEMPOTENT, idempotent.message());
        }
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            // 执行失败释放幂等锁，允许重试
            redis.delete(key);
            throw e;
        }
    }
}
