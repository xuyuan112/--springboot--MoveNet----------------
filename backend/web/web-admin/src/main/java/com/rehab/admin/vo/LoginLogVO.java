package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录/登出审计日志项（来源 operation_log，module='AUTH'）
 */
@Data
@Builder
@Schema(description = "登录/登出审计日志项")
public class LoginLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @Schema(description = "日志ID")
    private Long logId;

    /** 操作账号ID */
    @Schema(description = "操作账号ID")
    private Long userId;

    /** 操作类型：LOGIN/LOGOUT */
    @Schema(description = "操作类型：LOGIN/LOGOUT")
    private String action;

    /** 结果：SUCCESS/FAIL */
    @Schema(description = "结果：SUCCESS/FAIL")
    private String status;

    /** 客户端IP */
    @Schema(description = "客户端IP")
    private String ip;

    /** 设备标识 */
    @Schema(description = "设备标识")
    private String deviceId;

    /** 失败原因等说明（从 detail JSON 解析，解析失败回退原文） */
    @Schema(description = "失败原因等说明")
    private String message;

    /** 操作时间 */
    @Schema(description = "操作时间")
    private LocalDateTime createdAt;
}
