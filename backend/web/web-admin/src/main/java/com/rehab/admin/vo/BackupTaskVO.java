package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 备份任务 VO。confirmToken 仅在详情接口对可恢复备份一次性下发。
 */
@Data
@Schema(description = "备份任务")
public class BackupTaskVO {

    @Schema(description = "备份任务ID")
    private Long backupId;

    @Schema(description = "任务类型码值：BACKUP / PRE_RESTORE_SNAPSHOT")
    private String taskType;

    @Schema(description = "任务类型中文名")
    private String taskTypeName;

    @Schema(description = "任务状态码值：RUNNING / SUCCESS / FAILED")
    private String status;

    @Schema(description = "任务状态中文名")
    private String statusName;

    @Schema(description = "备份文件路径")
    private String filePath;

    @Schema(description = "备份文件大小（字节）")
    private Long fileSize;

    @Schema(description = "备份文件 SHA-256")
    private String sha256;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "触发操作人用户ID")
    private Long triggeredBy;

    @Schema(description = "触发操作人账号")
    private String operatorName;

    @Schema(description = "关联备份ID（恢复前快照）")
    private Long relatedBackupId;

    @Schema(description = "恢复原因")
    private String restoreReason;

    @Schema(description = "任务开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "任务结束时间")
    private LocalDateTime finishedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "恢复一次性确认令牌（仅详情接口返回，5 分钟内有效）")
    private String confirmToken;
}
