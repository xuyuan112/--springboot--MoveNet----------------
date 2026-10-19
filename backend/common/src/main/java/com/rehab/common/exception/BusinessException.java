package com.rehab.common.exception;

import com.rehab.common.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常：由 Service 层在业务规则不满足时主动抛出，
 * 经 {@link GlobalExceptionHandler} 转换为统一响应体，不打印错误堆栈。
 *
 * <p>异常同时携带业务码 {@link #code} 与 HTTP 状态 {@link #httpStatus}，
 * HTTP 状态默认取自 {@link ResultCode#getHttpStatus()}，与接口文档第 8 节一致。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务返回码（统一响应体中的 code） */
    private final String code;

    /** 对应的 HTTP 状态码 */
    private final int httpStatus;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.httpStatus = resultCode.getHttpStatus();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.httpStatus = resultCode.getHttpStatus();
    }

    public BusinessException(String code, String message) {
        this(code, message, 500);
    }

    public BusinessException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
