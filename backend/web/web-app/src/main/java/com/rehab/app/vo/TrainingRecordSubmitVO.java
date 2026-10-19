package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 训练汇总提交响应 VO：返回记录ID、任务新状态与是否重复提交（幂等标记）。
 */
@Schema(description = "训练汇总提交响应")
@Data
public class TrainingRecordSubmitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 训练记录ID（重复提交时返回首次记录ID） */
    @Schema(description = "训练记录ID（重复提交时返回首次记录ID）")
    private Long trainingRecordId;

    /** 每日任务ID */
    @Schema(description = "每日任务ID")
    private Long dailyTaskId;

    /** 任务落库状态（PENDING/IN_PROGRESS/COMPLETED/SKIPPED/EXPIRED） */
    @Schema(description = "任务落库状态")
    private String taskStatus;

    /** 任务目标次数 */
    @Schema(description = "任务目标次数")
    private Integer targetReps;

    /** 当前有效达标次数合计（不含 INSUFFICIENT 与已撤回记录） */
    @Schema(description = "当前有效达标次数合计")
    private Integer correctReps;

    /** 进度百分比（0-100） */
    @Schema(description = "进度百分比(0-100)")
    private BigDecimal progressPercent;

    /** 是否重复提交（相同 clientRequestId 命中首次结果时为 true） */
    @Schema(description = "是否重复提交")
    private Boolean duplicate;

    /** 本次记录质量是否为 INSUFFICIENT（不计入任务完成数与趋势） */
    @Schema(description = "本次记录质量是否为 INSUFFICIENT")
    private Boolean insufficient;
}
