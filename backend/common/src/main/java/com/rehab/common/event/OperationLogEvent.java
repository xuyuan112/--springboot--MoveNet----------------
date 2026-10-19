package com.rehab.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志事件：common 层只负责发布，不依赖具体 Mapper；
 * 由 web 层的事件监听器统一持久化到 operation_log，避免公共层反向依赖业务模块。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogEvent {

    /** 操作人用户 ID（登录失败等场景可能为空） */
    private Long userId;

    /** 业务模块 */
    private String module;

    /** 操作类型 */
    private String action;

    /** 目标对象类型 */
    private String targetType;

    /** 客户端 IP */
    private String ip;

    /** 设备标识 */
    private String deviceId;

    /** 执行结果：SUCCESS / FAIL */
    private String status;

    /** 参数/错误信息等详情 */
    private String detail;

    /** 发生时间 */
    private LocalDateTime occurredAt;
}
