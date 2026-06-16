package com.weizhenzu.infrastructure.aspect;

import com.weizhenzu.common.annotation.RateLimiter;
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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * 限流切面
 * <p>基于 Redis + Lua 实现滑动窗口限流</p>
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final StringRedisTemplate redis;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer pnd = new DefaultParameterNameDiscoverer();

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();

    static {
        SCRIPT.setScriptText(
            "local key = KEYS[1] " +
            "local count = tonumber(ARGV[1]) " +
            "local time = tonumber(ARGV[2]) " +
            "local current = redis.call('incr', key) " +
            "if current == 1 then redis.call('expire', key, time) end " +
            "if current > count then return 0 end " +
            "return 1"
        );
        SCRIPT.setResultType(Long.class);
    }

    @Around("@annotation(rl)")
    public Object around(ProceedingJoinPoint pjp, RateLimiter rl) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Object[] args = pjp.getArgs();
        String[] names = pnd.getParameterNames(method);

        EvaluationContext ctx = new StandardEvaluationContext();
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                ctx.setVariable(names[i], args[i]);
            }
        }
        Expression exp = parser.parseExpression(rl.key());
        Object value = exp.getValue(ctx);
        Long uid = UserContext.getUserId();
        String key = CommonConstants.RATE_LIMIT_KEY + value + ":" + (uid == null ? "anon" : uid);

        Long ok = redis.execute(SCRIPT, Collections.singletonList(key),
                String.valueOf(rl.count()), String.valueOf(rl.time()));
        if (ok == null || ok == 0) {
            throw new BizException(ResultCode.RATE_LIMIT, rl.message());
        }
        return pjp.proceed();
    }
}
