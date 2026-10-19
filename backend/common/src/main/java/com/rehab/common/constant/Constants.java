package com.rehab.common.constant;

/**
 * 平台通用常量
 */
public final class Constants {

    private Constants() {
    }

    /** 请求头：授权 */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 请求头：链路追踪 ID */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** MDC 中 traceId 的键名 */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /** 请求头：幂等键 */
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    /** 请求头：设备标识 */
    public static final String DEVICE_ID_HEADER = "X-Device-Id";

    /** 启用状态：是 */
    public static final Integer ENABLED = 1;

    /** 启用状态：否 */
    public static final Integer DISABLED = 0;

    /** 连续登录失败次数上限 */
    public static final int LOGIN_FAIL_LIMIT = 5;

    /** 账号锁定时长（分钟） */
    public static final long LOGIN_LOCK_MINUTES = 30L;

    /**
     * 操作成功状态（operation_log.status）。
     * 码值与 model 层 com.rehab.model.enums.OperationStatus.SUCCESS 对齐；
     * common 模块不依赖 model，故在此保留字符串常量供切面使用，业务层请直接引用枚举。
     */
    public static final String OP_SUCCESS = "SUCCESS";

    /**
     * 操作失败状态（operation_log.status）。
     * 码值与 model 层 com.rehab.model.enums.OperationStatus.FAIL 对齐。
     */
    public static final String OP_FAIL = "FAIL";

    /**
     * 医疗免责声明固定文案（AI 问答 / 评估报告统一使用，接口文档 3.4/6）。
     */
    public static final String MEDICAL_DISCLAIMER = "仅供参考，不能替代专业医疗建议";
}
