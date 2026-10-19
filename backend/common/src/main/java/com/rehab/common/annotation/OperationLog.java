package com.rehab.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 写操作方法上，
 * 由 {@code OperationLogAspect} 解析并发布事件，web 层监听器落库 operation_log。
 *
 * <p>module 建议取值：AUTH（登录认证）、USER（账号）、BINDING（绑定关系）、
 * ACTION（动作库）、PLAN（计划）、REPORT（报告）、CONTENT（内容）、
 * CONSULTATION（咨询）、NOTICE（公告）、DICT（字典）。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 业务模块 */
    String module();

    /** 操作类型，如 LOGIN / CREATE / UPDATE / DELETE / AUDIT / PUBLISH */
    String action();

    /** 操作对象类型，对应 operation_log.target_type */
    String targetType() default "";

    /** 是否记录请求参数（敏感接口如登录请置 false） */
    boolean recordParam() default false;
}
