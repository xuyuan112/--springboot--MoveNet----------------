package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 操作审计日志
 */
@Schema(description = "操作审计日志")
@Data
@TableName("operation_log")
public class OperationLog {

    /** 日志ID */
    @Schema(description = "日志ID")
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    /** 操作人用户ID（NULL为系统操作） */
    @Schema(description = "操作人用户ID（NULL为系统操作）")
    private Long userId;

    /** 模块名 */
    @Schema(description = "模块名")
    private String module;

    /** 操作类型 */
    @Schema(description = "操作类型")
    private String action;

    /** 目标对象类型 */
    @Schema(description = "目标对象类型")
    private String targetType;

    /** 目标对象ID */
    @Schema(description = "目标对象ID")
    private Long targetId;

    /** 客户端IP（兼容IPv6） */
    @Schema(description = "客户端IP（兼容IPv6）")
    private String ip;

    /** 设备标识 */
    @Schema(description = "设备标识")
    private String deviceId;

    /** 结果：SUCCESS/FAIL */
    @Schema(description = "结果：SUCCESS/FAIL")
    private String status;

    /** 操作详情（参数、变更前后值等），JSON 字符串 */
    @Schema(description = "操作详情（参数、变更前后值等），JSON 字符串")
    private String detail;

    /** 操作时间 */
    @Schema(description = "操作时间")
    private LocalDateTime createdAt;
}
