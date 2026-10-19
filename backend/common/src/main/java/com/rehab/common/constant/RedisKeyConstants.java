package com.rehab.common.constant;

/**
 * Redis 键名统一定义（冒号分层，便于检索与过期治理）。
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /** 登录会话：jwt:user:{userId} -> jti（值为当前有效令牌的唯一标识） */
    public static final String JWT_SESSION = "jwt:user:";

    /** 登录失败计数：auth:fail:{username} -> 次数 */
    public static final String LOGIN_FAIL_COUNT = "auth:fail:";

    /** 账号锁定标记：auth:lock:{username} */
    public static final String LOGIN_LOCK = "auth:lock:";

    /** 登录图形验证码：auth:captcha:{captchaId} -> 验证码文本（一次性消费，短 TTL） */
    public static final String CAPTCHA = "auth:captcha:";

    /** 接口幂等：idempotent:{key} */
    public static final String IDEMPOTENT = "idempotent:";

    /** 动作版本与规则缓存：cache:action:{versionId} */
    public static final String CACHE_ACTION = "cache:action:";

    /** 公共内容列表缓存：cache:content:{filterHash} */
    public static final String CACHE_CONTENT = "cache:content:";

    /** 文件直传会话：file:upload:{uploadId} -> 申报元数据 JSON（一次性消费，TTL 与预签名有效期一致） */
    public static final String FILE_UPLOAD = "file:upload:";

    /** 备份/恢复全局互斥锁：backup:lock（同一时刻仅允许一个备份或恢复任务） */
    public static final String BACKUP_LOCK = "backup:lock";

    /** 备份恢复一次性确认令牌：backup:confirm:{backupId} -> token（TTL 5 分钟，校验后删除） */
    public static final String BACKUP_CONFIRM = "backup:confirm:";

    /** 接口限流：rate:limit:{api}:{userId} */
    public static final String RATE_LIMIT = "rate:limit:";

    /** 每日任务生成调度锁：task:schedule:generate:{yyyy-MM-dd} */
    public static final String TASK_SCHEDULE_GENERATE = "task:schedule:generate:";

    /** 每日任务过期调度锁：task:schedule:expire:{yyyy-MM-dd} */
    public static final String TASK_SCHEDULE_EXPIRE = "task:schedule:expire:";

    /** 报告预生成调度锁：task:schedule:report:{yyyy-MM-dd} */
    public static final String TASK_SCHEDULE_REPORT = "task:schedule:report:";

    /** 患者进度今日聚合缓存：cache:progress:today:{userId}（写后失效） */
    public static final String PROGRESS_TODAY_CACHE = "cache:progress:today:";

    /** AI 问答单用户每日限流：rate:limit:ai:{userId} -> 次数 */
    public static final String AI_QA_RATE_LIMIT = "rate:limit:ai:";
}
