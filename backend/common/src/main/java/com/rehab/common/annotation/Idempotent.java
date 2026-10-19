package com.rehab.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等注解：请求必须携带 {@code Idempotency-Key} 请求头，
 * 在有效期内相同键的第二次请求将被拒绝。
 *
 * <p>注意：训练提交的强幂等由数据库唯一约束
 * {@code uk_record_idempotency(daily_task_id, client_request_id)} 保证，
 * 本注解用于通用写操作（如创建计划、发起咨询）的防重复点击。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /** 幂等键有效期，默认 10 分钟 */
    long expire() default 10L;

    /** 时间单位 */
    TimeUnit timeUnit() default TimeUnit.MINUTES;
}
