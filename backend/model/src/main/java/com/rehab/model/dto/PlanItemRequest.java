package com.rehab.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 计划动作项新增/修改请求（POST、PUT /therapist/plans/{planId}/items[/{itemId}]）
 */
@Data
@Schema(description = "计划动作项新增/修改请求（POST、PUT /therapist/plans/{planId}/items[/{itemId}]）")
public class PlanItemRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 动作版本ID（必须为已发布版本） */
    @Schema(description = "动作版本ID（必须为已发布版本）", example = "7")
    @NotNull(message = "动作版本ID不能为空")
    private Long actionVersionId;

    /** 动作顺序号（同计划内升序） */
    @Schema(description = "动作顺序号（同计划内升序）", example = "1")
    @Min(value = 0, message = "顺序号不能为负")
    private Integer sequenceNo;

    /** 目标组数 */
    @Schema(description = "目标组数", example = "3")
    @NotNull(message = "目标组数不能为空")
    @Min(value = 1, message = "目标组数至少为 1")
    private Integer targetSets;

    /** 目标次数/组 */
    @Schema(description = "目标次数/组", example = "10")
    @NotNull(message = "目标次数不能为空")
    @Min(value = 1, message = "目标次数至少为 1")
    private Integer targetReps;

    /** 目标幅度（度） */
    @Schema(description = "目标幅度（度）", example = "90")
    private BigDecimal targetAmplitude;

    /** 单组时长（秒） */
    @Schema(description = "单组时长（秒）", example = "20")
    @Min(value = 0, message = "单组时长不能为负")
    private Integer durationSec;

    /** 组间休息（秒） */
    @Schema(description = "组间休息（秒）", example = "30")
    @Min(value = 0, message = "休息时长不能为负")
    private Integer restSec;

    /** 动作指导语 */
    @Schema(description = "动作指导语")
    @Size(max = 500, message = "指导语最长 500 个字符")
    private String instruction;

}
