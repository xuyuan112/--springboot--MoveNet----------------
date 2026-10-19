package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 患者端计划动作项 VO。
 */
@Schema(description = "患者端计划动作项")
@Data
public class PatientPlanItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 计划项ID */
    @Schema(description = "计划项ID")
    private Long planItemId;

    /** 计划ID */
    @Schema(description = "计划ID")
    private Long planId;

    /** 动作版本ID */
    @Schema(description = "动作版本ID")
    private Long actionVersionId;

    /** 动作ID */
    @Schema(description = "动作ID")
    private Long actionId;

    /** 动作名称 */
    @Schema(description = "动作名称")
    private String actionName;

    /** 版本号 */
    @Schema(description = "版本号")
    private Integer versionNo;

    /** 动作顺序号 */
    @Schema(description = "动作顺序号")
    private Integer sequenceNo;

    /** 目标组数 */
    @Schema(description = "目标组数")
    private Integer targetSets;

    /** 目标次数/组 */
    @Schema(description = "目标次数/组")
    private Integer targetReps;

    /** 目标幅度(度) */
    @Schema(description = "目标幅度(度)")
    private BigDecimal targetAmplitude;

    /** 单组时长(秒) */
    @Schema(description = "单组时长(秒)")
    private Integer durationSec;

    /** 组间休息(秒) */
    @Schema(description = "组间休息(秒)")
    private Integer restSec;

    /** 动作指导语 */
    @Schema(description = "动作指导语")
    private String instruction;
}
