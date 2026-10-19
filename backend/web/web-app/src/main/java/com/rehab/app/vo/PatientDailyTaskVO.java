package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者端每日训练任务 VO。
 */
@Schema(description = "患者端每日训练任务")
@Data
public class PatientDailyTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    @Schema(description = "任务ID")
    private Long taskId;

    /** 计划项ID */
    @Schema(description = "计划项ID")
    private Long planItemId;

    /** 计划ID */
    @Schema(description = "计划ID")
    private Long planId;

    /** 计划名称 */
    @Schema(description = "计划名称")
    private String planName;

    /** 动作名称 */
    @Schema(description = "动作名称")
    private String actionName;

    /** 任务日期 */
    @Schema(description = "任务日期")
    private LocalDate taskDate;

    /** 目标组数（生成任务时快照） */
    @Schema(description = "目标组数（生成任务时快照）")
    private Integer targetSets;

    /** 目标次数（生成任务时快照） */
    @Schema(description = "目标次数（生成任务时快照）")
    private Integer targetReps;

    /** 已达标次数（根据训练记录计算） */
    @Schema(description = "已达标次数（根据训练记录计算）")
    private Integer correctReps;

    /** 状态：PENDING/IN_PROGRESS/COMPLETED/PARTIAL/SKIPPED/EXPIRED */
    @Schema(description = "状态：PENDING/IN_PROGRESS/COMPLETED/PARTIAL/SKIPPED/EXPIRED")
    private String status;

    /** 状态名称 */
    @Schema(description = "状态名称")
    private String statusName;

    /** 进度百分比(0-100) */
    @Schema(description = "进度百分比(0-100)")
    private BigDecimal progressPercent;

    /** 跳过原因 */
    @Schema(description = "跳过原因")
    private String skipReason;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    /** 完成时间 */
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
}
