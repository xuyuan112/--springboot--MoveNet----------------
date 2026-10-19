package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端操作审计日志 VO。
 */
@Data
@Schema(description = "操作审计日志")
public class AdminOperationLogVO {

    @Schema(description = "日志ID")
    private Long logId;

    @Schema(description = "操作人用户ID（NULL 为系统操作）")
    private Long userId;

    @Schema(description = "操作人账号")
    private String userName;

    @Schema(description = "模块名：AUTH/USER/BINDING/ACTION/CONTENT/NOTICE/DICT/BACKUP 等")
    private String module;

    @Schema(description = "操作类型：LOGIN/CREATE/UPDATE/DELETE/PUBLISH 等")
    private String action;

    @Schema(description = "目标对象类型")
    private String targetType;

    @Schema(description = "目标对象ID")
    private Long targetId;

    @Schema(description = "客户端IP")
    private String ip;

    @Schema(description = "设备标识")
    private String deviceId;

    @Schema(description = "结果：SUCCESS/FAIL")
    private String status;

    @Schema(description = "操作详情 JSON（请求参数或失败原因）")
    private String detail;

    @Schema(description = "操作时间")
    private LocalDateTime createdAt;
}
