package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库备份恢复任务
 */
@Schema(description = "数据库备份恢复任务")
@Data
@TableName("db_backup_task")
public class DbBackupTask {

    /** 备份任务ID */
    @Schema(description = "备份任务ID")
    @TableId(value = "backup_id", type = IdType.AUTO)
    private Long backupId;

    /** 任务类型：BACKUP 手工备份；PRE_RESTORE_SNAPSHOT 恢复前自动快照 */
    @Schema(description = "任务类型：BACKUP / PRE_RESTORE_SNAPSHOT")
    private String taskType;

    /** 任务状态：RUNNING / SUCCESS / FAILED */
    @Schema(description = "任务状态：RUNNING / SUCCESS / FAILED")
    private String status;

    /** 备份产物绝对路径（.sql） */
    @Schema(description = "备份产物绝对路径")
    private String filePath;

    /** 备份文件大小（字节） */
    @Schema(description = "备份文件大小（字节）")
    private Long fileSize;

    /** 备份文件 SHA-256 摘要 */
    @Schema(description = "备份文件 SHA-256 摘要")
    private String sha256;

    /** 失败原因 */
    @Schema(description = "失败原因")
    private String errorMsg;

    /** 触发操作人用户ID（系统任务为 NULL） */
    @Schema(description = "触发操作人用户ID")
    private Long triggeredBy;

    /** 关联备份ID（恢复前快照指向被恢复的备份） */
    @Schema(description = "关联备份ID")
    private Long relatedBackupId;

    /** 恢复原因（仅恢复前快照记录） */
    @Schema(description = "恢复原因")
    private String restoreReason;

    /** 任务开始时间 */
    @Schema(description = "任务开始时间")
    private LocalDateTime startedAt;

    /** 任务结束时间 */
    @Schema(description = "任务结束时间")
    private LocalDateTime finishedAt;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
