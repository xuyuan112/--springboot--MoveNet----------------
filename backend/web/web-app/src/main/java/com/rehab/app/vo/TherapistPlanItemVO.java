package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 治疗师端计划动作项 VO（GET /therapist/plans/{planId}/items 及计划详情内嵌）。
 */
@Data
@Schema(description = "治疗师端计划动作项")
public class TherapistPlanItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "计划项ID")
    private Long planItemId;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "动作版本ID")
    private Long actionVersionId;

    @Schema(description = "动作ID")
    private Long actionId;

    @Schema(description = "动作名称")
    private String actionName;

    @Schema(description = "动作版本号")
    private Integer versionNo;

    @Schema(description = "动作顺序号")
    private Integer sequenceNo;

    @Schema(description = "目标组数")
    private Integer targetSets;

    @Schema(description = "目标次数/组")
    private Integer targetReps;

    @Schema(description = "目标幅度(度)")
    private BigDecimal targetAmplitude;

    @Schema(description = "单组时长(秒)")
    private Integer durationSec;

    @Schema(description = "组间休息(秒)")
    private Integer restSec;

    @Schema(description = "动作指导语")
    private String instruction;
}
