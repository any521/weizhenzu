package com.weizhenzu.infrastructure.aspect;

import com.weizhenzu.common.annotation.DistributedLock;
import com.weizhenzu.common.constant.CommonConstants;
import com.weizhenzu.common.exception.BizException;
import com.weizhenzu.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 分布式锁切面
 * <p>基于 Redisson 实现</p>
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redisson;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer pnd = new DefaultParameterNameDiscoverer();

    @Around("@annotation(dl)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock dl) throws Throwable {
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
        Expression exp = parser.parseExpression(dl.key());
        Object value = exp.getValue(ctx);
        String key = CommonConstants.LOCK_KEY + value;

        RLock lock = redisson.getLock(key);
        boolean acquired;
        try {
            acquired = lock.tryLock(dl.waitTime(), dl.leaseTime(), dl.timeUnit());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.FAIL, "加锁失败");
        }
        if (!acquired) {
            throw new BizException(ResultCode.RATE_LIMIT, dl.message());
        }
        try {
            return pjp.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
