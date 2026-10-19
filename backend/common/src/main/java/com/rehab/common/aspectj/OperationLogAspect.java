package com.rehab.common.aspectj;

import cn.hutool.json.JSONUtil;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.constant.Constants;
import com.rehab.common.event.OperationLogEvent;
import com.rehab.common.security.LoginUser;
import com.rehab.common.security.SecurityUtils;
import com.rehab.common.util.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面：拦截 {@link OperationLog} 注解方法，
 * 无论成功失败都发布 {@link OperationLogEvent}（发布动作本身不影响主业务事务）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final ApplicationEventPublisher eventPublisher;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        String status = Constants.OP_SUCCESS;
        String detail = null;
        Object result = null;
        Throwable thrown = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = Constants.OP_FAIL;
            detail = e.getMessage();
            thrown = e;
            throw e;
        } finally {
            try {
                publishEvent(joinPoint, operationLog, status, detail);
            } catch (Exception publishEx) {
                // 日志失败绝不影响业务
                log.warn("操作日志事件发布失败：{}", publishEx.getMessage());
            }
            if (thrown != null) {
                log.debug("操作日志已记录失败操作 module={}, action={}",
                        operationLog.module(), operationLog.action());
            }
        }
    }

    private void publishEvent(ProceedingJoinPoint joinPoint, OperationLog annotation,
                              String status, String errorDetail) {
        Long userId = null;
        try {
            if (SecurityUtils.isAuthenticated()) {
                LoginUser loginUser = SecurityUtils.getLoginUser();
                userId = loginUser.getUserId();
            }
        } catch (Exception ignored) {
            // 未登录场景（如登录失败）userId 留空
        }

        String detail = null;
        if (Constants.OP_SUCCESS.equals(status) && annotation.recordParam()) {
            detail = buildParamJson(joinPoint);
        } else if (Constants.OP_FAIL.equals(status) && StringUtils.hasText(errorDetail)) {
            // detail 为 MySQL JSON 列，失败原因必须包装为合法 JSON
            detail = JSONUtil.createObj().set("error", errorDetail).toString();
        }

        OperationLogEvent event = OperationLogEvent.builder()
                .userId(userId)
                .module(annotation.module())
                .action(annotation.action())
                .targetType(annotation.targetType())
                .ip(ServletUtils.currentIp())
                .deviceId(ServletUtils.currentDeviceId())
                .status(status)
                .detail(detail)
                .occurredAt(LocalDateTime.now())
                .build();
        eventPublisher.publishEvent(event);
    }

    /**
     * 序列化方法参数，剔除 Servlet 原生对象与 MultipartFile，避免循环序列化。
     * 注意：operation_log.detail 为 MySQL JSON 列，必须输出合法 JSON（含错误兜底）。
     */
    private String buildParamJson(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] names = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            cn.hutool.json.JSONObject obj = JSONUtil.createObj();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (isIgnorable(arg)) {
                    continue;
                }
                String name = (names != null && i < names.length) ? names[i] : ("arg" + i);
                obj.set(name, arg);
            }
            return obj.toString();
        } catch (Exception e) {
            // 兜底也必须是合法 JSON，否则 detail（JSON 列）插入会被 MySQL 拒绝
            return JSONUtil.createObj().set("serializeError", Arrays.toString(joinPoint.getArgs())).toString();
        }
    }

    private boolean isIgnorable(Object arg) {
        return arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof org.springframework.web.multipart.MultipartFile;
    }
}
