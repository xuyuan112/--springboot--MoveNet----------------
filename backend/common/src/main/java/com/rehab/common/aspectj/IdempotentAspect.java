package com.rehab.common.aspectj;

import com.rehab.common.annotation.Idempotent;
import com.rehab.common.constant.Constants;
import com.rehab.common.constant.RedisKeyConstants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 幂等切面：基于 Redis SET NX EX 实现，键存在即视为重复提交。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = com.rehab.common.util.ServletUtils.getRequest();
        String idempotencyKey = request == null ? null : request.getHeader(Constants.IDEMPOTENCY_HEADER);
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ResultCode.MISSING_PARAMETER, "缺少幂等请求头 Idempotency-Key");
        }

        String redisKey = RedisKeyConstants.IDEMPOTENT + idempotencyKey;
        Boolean firstSubmit = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", idempotent.expire(), idempotent.timeUnit());
        if (!Boolean.TRUE.equals(firstSubmit)) {
            throw new BusinessException(ResultCode.IDEMPOTENCY_CONFLICT);
        }
        return joinPoint.proceed();
    }
}
