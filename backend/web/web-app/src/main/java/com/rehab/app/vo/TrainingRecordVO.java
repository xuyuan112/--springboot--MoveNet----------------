package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 患者端训练记录列表项 VO（不含原始视频与逐帧关键点，不含病史）。
 */
@Schema(description = "患者端训练记录列表项")
@Data
public class TrainingRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @Schema(description = "记录ID")
    private Long recordId;

    /** 每日任务ID */
    @Schema(description = "每日任务ID")
    private Long dailyTaskId;

    /** 计划ID */
    @Schema(description = "计划ID")
    private Long planId;

    /** 计划名称 */
    @Schema(description = "计划名称")
    private String planName;

    /** 计划动作项ID */
    @Schema(description = "计划动作项ID")
    private Long planItemId;

    /** 动作版本ID */
    @Schema(description = "动作版本ID")
    private Long actionVersionId;

    /** 动作名称 */
    @Schema(description = "动作名称")
    private String actionName;

    /** 训练开始时间 */
    @Schema(description = "训练开始时间")
    private LocalDateTime startedAt;

    /** 训练结束时间 */
    @Schema(description = "训练结束时间")
    private LocalDateTime endedAt;

    /** 总次数 */
    @Schema(description = "总次数")
    private Integer totalReps;

    /** 标准（正确）次数 */
    @Schema(description = "标准（正确）次数")
    private Integer correctReps;

    /** 综合得分(0-100) */
    @Schema(description = "综合得分(0-100)")
    private BigDecimal totalScore;

    /** 姿态质量等级（GOOD/FAIR/POOR/INSUFFICIENT） */
    @Schema(description = "姿态质量等级")
    private String poseQuality;

    /** 有效帧占比(0-1) */
    @Schema(description = "有效帧占比(0-1)")
    private BigDecimal validFrameRatio;

    /** 关键点修复帧占比(0-1) */
    @Schema(description = "关键点修复帧占比(0-1)")
    private BigDecimal poseRepairedRatio;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
