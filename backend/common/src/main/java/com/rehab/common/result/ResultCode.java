package com.rehab.common.result;

import lombok.Getter;

/**
 * 统一业务返回码。
 *
 * <p>码值与 HTTP 状态严格对齐《接口文档.md》第 8 节错误码表：</p>
 * <ul>
 *   <li>{@link #SUCCESS} 的 code 为 {@code "0"}，前端据此判定成功；</li>
 *   <li>其余错误码为大写下划线语义字符串（如 {@code VALIDATION_ERROR}），
 *       不再使用早期的 P/A/D/B+数字 方案；</li>
 *   <li>每个错误码携带对应的 HTTP 状态 {@code httpStatus}，
 *       由 GlobalExceptionHandler / Security 处理器写入响应状态行，
 *       业务码写在统一响应体 {@link ApiResult#getCode()} 中。</li>
 * </ul>
 *
 * <p>新增错误码时必须先在接口文档登记，保持前后端契约唯一来源。</p>
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "0", "OK"),

    // ==================== 400 请求参数错误 ====================
    /** 请求体、查询参数或日期格式错误；可携带 fieldErrors */
    VALIDATION_ERROR(400, "VALIDATION_ERROR", "请求参数错误"),
    /** 缺少必填参数 */
    MISSING_PARAMETER(400, "MISSING_PARAMETER", "缺少必填参数"),
    /** 枚举值不在允许集合内 */
    INVALID_ENUM(400, "INVALID_ENUM", "枚举值不合法"),
    /** 数值超出允许范围 */
    INVALID_RANGE(400, "INVALID_RANGE", "数值超出允许范围"),
    /**
     * 平台补充码（图形验证码，错误码表未单列）：
     * 验证码错误或已过期，前端应刷新验证码后重试，HTTP 返回 400。
     */
    CAPTCHA_INVALID(400, "CAPTCHA_INVALID", "验证码错误或已过期，请刷新后重试"),

    // ==================== 401 未认证 ====================
    /** 未携带 Bearer Token 或会话已失效（入口认证失败的统一码） */
    AUTH_REQUIRED(401, "AUTH_REQUIRED", "未登录或登录状态已过期"),
    /**
     * 平台补充码（文档第 2 节登录失败场景，错误码表未单列）：
     * 用户名或密码错误，HTTP 仍返回 401。
     */
    LOGIN_FAILED(401, "LOGIN_FAILED", "用户名或密码错误"),
    /** 令牌已过期，前端应引导重新登录 */
    TOKEN_EXPIRED(401, "TOKEN_EXPIRED", "登录令牌已过期，请重新登录"),
    /** 令牌签名非法或格式错误 */
    TOKEN_INVALID(401, "TOKEN_INVALID", "登录令牌无效"),
    /** 账号已被管理员禁用 */
    ACCOUNT_DISABLED(401, "ACCOUNT_DISABLED", "账号已被禁用，请联系管理员"),
    /** 连续登录失败触发的时间窗锁定 */
    ACCOUNT_LOCKED(401, "ACCOUNT_LOCKED", "登录失败次数过多，账号已锁定 30 分钟"),

    // ==================== 403 无权限 / 归属校验 ====================
    /** 已登录但角色不允许执行该操作 */
    FORBIDDEN(403, "FORBIDDEN", "没有访问权限"),
    /** 患者/治疗师/资源不属于当前有效绑定关系 */
    OWNERSHIP_DENIED(403, "OWNERSHIP_DENIED", "资源不属于当前用户或绑定关系已失效"),
    /** 无权访问病史、原始关键点等敏感数据 */
    SENSITIVE_DATA_DENIED(403, "SENSITIVE_DATA_DENIED", "无权访问该敏感数据"),

    // ==================== 404 资源不存在 ====================
    /** 资源不存在或对当前用户不可见 */
    NOT_FOUND(404, "NOT_FOUND", "请求的资源不存在"),
    /** 动作版本不存在 */
    VERSION_NOT_FOUND(404, "VERSION_NOT_FOUND", "动作版本不存在"),
    /** 每日任务不存在 */
    TASK_NOT_FOUND(404, "TASK_NOT_FOUND", "每日任务不存在"),
    /** MinIO/业务文件不存在 */
    FILE_NOT_FOUND(404, "FILE_NOT_FOUND", "文件不存在"),
    /** 预签名 URL 已过期，需重新获取 */
    FILE_URL_EXPIRED(404, "FILE_URL_EXPIRED", "文件访问地址已过期，请重新获取"),

    // ==================== 409 冲突 / 幂等 / 状态机 ====================
    /** 唯一字段冲突（账号、绑定、收藏等） */
    DUPLICATE_RESOURCE(409, "DUPLICATE_RESOURCE", "资源已存在，请勿重复提交"),
    /** 相同 client_request_id / Idempotency-Key 已处理，返回首次结果 */
    IDEMPOTENCY_CONFLICT(409, "IDEMPOTENCY_CONFLICT", "相同幂等键的请求已处理，请勿重复提交"),
    /** 当前状态不允许该操作 */
    STATE_CONFLICT(409, "STATE_CONFLICT", "当前状态不允许该操作"),
    /** 已发布的动作/计划/报告版本不可修改 */
    VERSION_IMMUTABLE(409, "VERSION_IMMUTABLE", "已发布版本不可修改"),
    /** 同计划同周期报告唯一 */
    REPORT_ALREADY_EXISTS(409, "REPORT_ALREADY_EXISTS", "同计划同周期的报告已存在"),
    /** 主责治疗师唯一性或绑定有效期冲突 */
    BINDING_CONFLICT(409, "BINDING_CONFLICT", "主责治疗师或绑定有效期冲突"),
    /** 文件已被内容、模板或消息引用，不可删除 */
    FILE_IN_USE(409, "FILE_IN_USE", "文件已被引用，无法删除"),

    // ==================== 413 / 415 文件约束 ====================
    /** 文件超过用途对应大小上限 */
    FILE_TOO_LARGE(413, "FILE_TOO_LARGE", "文件大小超过允许上限"),
    /** MIME 类型不在白名单 */
    UNSUPPORTED_MEDIA_TYPE(415, "UNSUPPORTED_MEDIA_TYPE", "文件类型不支持"),

    // ==================== 422 训练与文件业务校验 ====================
    /** 合格帧比例不足或修复比例超限：只记录不计分 */
    POSE_QUALITY_INSUFFICIENT(422, "POSE_QUALITY_INSUFFICIENT", "姿态质量不足，本次训练只记录不计分"),
    /** 未通过 motion_signature 校验：不计数不评分 */
    ACTION_MISMATCH(422, "ACTION_MISMATCH", "动作特征不匹配，本次训练不计入"),
    /** 分数、比例、次数或算法指标不合法 */
    METRIC_INVALID(422, "METRIC_INVALID", "训练指标不合法"),
    /** pose_engine_id 与动作版本要求不一致 */
    ENGINE_VERSION_MISMATCH(422, "ENGINE_VERSION_MISMATCH", "姿态引擎版本与动作版本不一致"),
    /** 模板缺失，相似度降权为 0（通常不阻塞训练） */
    TEMPLATE_UNAVAILABLE(422, "TEMPLATE_UNAVAILABLE", "标准模板暂不可用，相似度得分按 0 处理"),
    /** 计划动作参数超出动作版本允许范围 */
    PLAN_PARAMETER_INVALID(422, "PLAN_PARAMETER_INVALID", "计划动作参数超出允许范围"),
    /** 报告缺少数据或不在待审核状态 */
    REPORT_NOT_REVIEWABLE(422, "REPORT_NOT_REVIEWABLE", "报告当前状态不允许审核或数据不完整"),
    /** 上传对象 SHA-256 与申报值不一致 */
    FILE_HASH_MISMATCH(422, "FILE_HASH_MISMATCH", "文件哈希校验不一致"),
    /** 文件安全扫描未通过 */
    FILE_SCAN_FAILED(422, "FILE_SCAN_FAILED", "文件安全扫描未通过"),

    // ==================== 423 / 424 锁定与依赖 ====================
    /** 账号、计划或备份正被其他操作锁定 */
    RESOURCE_LOCKED(423, "RESOURCE_LOCKED", "资源正在被其他操作处理，请稍后再试"),
    /** MinIO 等存储依赖不可用；不丢失业务数据 */
    STORAGE_UNAVAILABLE(424, "STORAGE_UNAVAILABLE", "文件存储暂不可用，请稍后重试"),

    // ==================== 429 限流 ====================
    /** 请求频率超限，可按 Retry-After 等待 */
    RATE_LIMITED(429, "RATE_LIMITED", "请求过于频繁，请稍后再试"),

    // ==================== 5xx 服务端 / 上游 ====================
    /** 未分类服务端异常，提示模糊化并携带 traceId */
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "系统繁忙，请稍后重试"),
    /** AI、MinIO 等依赖服务返回错误 */
    UPSTREAM_ERROR(502, "UPSTREAM_ERROR", "上游服务异常"),
    /** 服务维护或暂时不可用 */
    SERVICE_UNAVAILABLE(503, "SERVICE_UNAVAILABLE", "服务暂不可用"),
    /** 依赖服务超时，可安全重试（写操作重试须复用同一幂等键） */
    UPSTREAM_TIMEOUT(504, "UPSTREAM_TIMEOUT", "上游服务处理超时，请稍后重试");

    /** 对应的 HTTP 状态码 */
    private final int httpStatus;

    /** 业务码（成功为 "0"，失败为大写下划线语义字符串） */
    private final String code;

    /** 默认提示信息 */
    private final String message;

    ResultCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
