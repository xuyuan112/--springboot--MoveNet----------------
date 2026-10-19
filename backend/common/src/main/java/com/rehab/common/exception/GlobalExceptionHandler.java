package com.rehab.common.exception;

import com.rehab.common.result.ApiResult;
import com.rehab.common.result.ResultCode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把各类异常统一转换为 {@link ApiResult}。
 *
 * <p>约定（与接口文档第 8 节一致）：HTTP 状态行与业务码同时表达错误语义，
 * 例如重复提交返回 {@code HTTP 409 + body.code=IDEMPOTENCY_CONFLICT}。</p>
 *
 * <p>注意：认证/授权在 Spring Security 过滤器链中发生的异常由
 * SecurityConfig 中配置的 EntryPoint / AccessDeniedHandler 处理（返回真实 HTTP 401/403），
 * 此处只兜底到达 DispatcherServlet 之后的异常。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常（可预期，不打印堆栈）；HTTP 状态取自 ResultCode */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 uri={}, code={}, message={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus())
                .body(ApiResult.fail(e.getCode(), e.getMessage()));
    }

    /** @RequestBody 校验失败：400 VALIDATION_ERROR */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ApiResult.fail(ResultCode.VALIDATION_ERROR, message);
    }

    /** 表单绑定校验失败：400 VALIDATION_ERROR */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBind(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ApiResult.fail(ResultCode.VALIDATION_ERROR, message);
    }

    /** 方法参数 / 路径变量约束校验失败：400 VALIDATION_ERROR */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleConstraint(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return ApiResult.fail(ResultCode.VALIDATION_ERROR, message);
    }

    /** 缺少必填请求参数：400 MISSING_PARAMETER */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return ApiResult.fail(ResultCode.MISSING_PARAMETER, "缺少必填参数：" + e.getParameterName());
    }

    /** 请求体不可读 / JSON 格式错误：400 VALIDATION_ERROR */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        // 枚举入参反序列化失败（如 groupCode 传了不存在的码值）：定位到具体字段和错误值
        if (e.getCause() instanceof InvalidFormatException ife && ife.getTargetType().isEnum()) {
            String field = ife.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse("参数");
            return ApiResult.fail(ResultCode.VALIDATION_ERROR,
                    "字段 " + field + " 的取值非法：" + ife.getValue());
        }
        return ApiResult.fail(ResultCode.VALIDATION_ERROR, "请求体格式错误或缺失");
    }

    /** 请求方法不支持：405（文档错误码表未单列，沿用参数错误码 + 明确提示） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResult<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ApiResult.fail(ResultCode.VALIDATION_ERROR, "不支持的请求方法：" + e.getMethod());
    }

    /** 接口不存在（需配置 throw-exception-if-no-handler-found）：404 NOT_FOUND */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResult<Void> handleNotFound(NoHandlerFoundException e) {
        return ApiResult.fail(ResultCode.NOT_FOUND);
    }

    /** 已登录但权限不足：403 FORBIDDEN */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleAccessDenied(AccessDeniedException e) {
        return ApiResult.fail(ResultCode.FORBIDDEN);
    }

    /** 安全上下文缺失（个别容器内转发场景兜底）：401 AUTH_REQUIRED */
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleAuthNotFound(AuthenticationCredentialsNotFoundException e) {
        return ApiResult.fail(ResultCode.AUTH_REQUIRED);
    }

    /** 未预期异常：打印完整堆栈，返回模糊提示，避免泄露内部信息：500 INTERNAL_ERROR */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 uri={}", request.getRequestURI(), e);
        return ApiResult.fail(ResultCode.INTERNAL_ERROR);
    }
}
