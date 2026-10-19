package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 治疗师端患者每日任务 VO（GET /therapist/patients/{patientId}/tasks）。
 */
@Data
@Schema(description = "治疗师端患者每日任务项")
public class TherapistPatientTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务日期")
    private LocalDate taskDate;

    @Schema(description = "状态：PENDING/IN_PROGRESS/COMPLETED/SKIPPED/EXPIRED")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "动作名称")
    private String actionName;

    @Schema(description = "动作顺序号")
    private Integer sequenceNo;

    @Schema(description = "目标组数（生成时快照）")
    private Integer targetSets;

    @Schema(description = "目标次数/组（生成时快照）")
    private Integer targetReps;
}
