package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 每日训练任务
 */
@Schema(description = "每日训练任务")
@Data
@TableName("plan_daily_task")
public class PlanDailyTask {

    /** 任务ID */
    @Schema(description = "任务ID")
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    /** 计划项ID */
    @Schema(description = "计划项ID")
    private Long planItemId;

    /** 任务日期 */
    @Schema(description = "任务日期")
    private LocalDate taskDate;

    /** 目标组数（生成任务时快照） */
    @Schema(description = "目标组数（生成任务时快照）")
    private Integer targetSets;

    /** 目标次数（生成任务时快照） */
    @Schema(description = "目标次数（生成任务时快照）")
    private Integer targetReps;

    /** 状态：PENDING/IN_PROGRESS/COMPLETED/SKIPPED/EXPIRED */
    @Schema(description = "状态：PENDING/IN_PROGRESS/COMPLETED/SKIPPED/EXPIRED")
    private String status;

    /** 跳过原因 */
    @Schema(description = "跳过原因")
    private String skipReason;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    /** 完成时间 */
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /** 逻辑删除：0正常1已删 */
    @Schema(description = "逻辑删除：0正常1已删")
    @TableLogic
    private Integer deleted;
}
