package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者任务完成明细行（P5-B1 /patient/progress 分页部分）。
 */
@Data
@Schema(description = "患者任务完成明细")
public class PatientTaskProgressVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务日期")
    private LocalDate taskDate;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "计划项ID")
    private Long planItemId;

    @Schema(description = "动作名称")
    private String actionName;

    @Schema(description = "目标组数")
    private Integer targetSets;

    @Schema(description = "目标次数")
    private Integer targetReps;

    @Schema(description = "有效达标次数（排除 INSUFFICIENT）")
    private Integer correctReps;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "任务状态名称")
    private String statusName;

    @Schema(description = "进度百分比")
    private BigDecimal progressPercent;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
}
