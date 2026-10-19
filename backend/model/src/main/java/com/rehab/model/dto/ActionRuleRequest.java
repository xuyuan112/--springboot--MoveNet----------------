package com.rehab.model.dto;

import com.rehab.model.enums.ActionPhase;
import com.rehab.model.enums.AssessmentDimension;
import com.rehab.model.enums.JointRole;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 动作评估规则新增/修改请求（/admin/action-versions/{versionId}/rules，发布后只读）
 */
@Data
@Schema(description = "动作评估规则新增/修改请求（/admin/action-versions/{versionId}/rules，发布后只读）")
public class ActionRuleRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关节名称（如 left_knee） */
    @Schema(description = "关节名称（如 left_knee）", example = "left_knee")
    @NotBlank(message = "关节名称不能为空")
    @Size(max = 50, message = "关节名称最长 50 个字符")
    private String jointName;

    /** 参与计算的关键点索引 JSON */
    @Schema(description = "参与计算的关键点索引 JSON", example = "[23,25,27]")
    @Size(max = 500, message = "关键点索引最长 500 个字符")
    private String keypointIndices;

    /** 角度计算公式 */
    @Schema(description = "角度计算公式")
    @Size(max = 255, message = "计算公式最长 255 个字符")
    private String angleFormula;

    /** 关节角色：PRIMARY 主评估 / AUXILIARY 辅助 */
    @Schema(description = "关节角色：PRIMARY / AUXILIARY")
    @NotNull(message = "关节角色不能为空")
    private JointRole jointRole;

    /** 评估维度：ANGLE/STABILITY/SYMMETRY/HOLD/SPEED */
    @Schema(description = "评估维度：ANGLE/STABILITY/SYMMETRY/HOLD/SPEED")
    @NotNull(message = "评估维度不能为空")
    private AssessmentDimension dimension;

    /** 动作阶段：START/CONCENTRIC/ECCENTRIC/HOLD/FULL */
    @Schema(description = "动作阶段：START/CONCENTRIC/ECCENTRIC/HOLD/FULL")
    @NotNull(message = "动作阶段不能为空")
    private ActionPhase phase;

    /** 最小角度（度） */
    @Schema(description = "最小角度（度）")
    private BigDecimal minAngle;

    /** 最大角度（度） */
    @Schema(description = "最大角度（度）")
    private BigDecimal maxAngle;

    /** 标准角度（度） */
    @Schema(description = "标准角度（度）")
    private BigDecimal standardAngle;

    /** 软容差（提示级，度） */
    @Schema(description = "软容差（提示级，度）")
    private BigDecimal toleranceSoft;

    /** 硬容差（纠错级，度） */
    @Schema(description = "硬容差（纠错级，度）")
    private BigDecimal toleranceHard;

    /** 保持所需帧数 */
    @Schema(description = "保持所需帧数")
    @Min(value = 0, message = "保持帧数不能为负")
    private Integer holdFramesRequired;

    /** 最小速度（度/秒） */
    @Schema(description = "最小速度（度/秒）")
    private BigDecimal speedMin;

    /** 最大速度（度/秒） */
    @Schema(description = "最大速度（度/秒）")
    private BigDecimal speedMax;

    /** 评分权重，范围 0~1 */
    @Schema(description = "评分权重，范围 0~1")
    @DecimalMin(value = "0", message = "权重需在 0~1 之间")
    @DecimalMax(value = "1", message = "权重需在 0~1 之间")
    private BigDecimal scoreWeight;

    /** 严重等级（数值越大越严重） */
    @Schema(description = "严重等级（数值越大越严重）")
    @Min(value = 1, message = "严重等级最小为 1")
    private Integer severity;

    /** 纠错提示文案 */
    @Schema(description = "纠错提示文案")
    @Size(max = 255, message = "提示文案最长 255 个字符")
    private String promptText;

    /** 对称评估配对规则ID（同版本内，可空） */
    @Schema(description = "对称评估配对规则ID（同版本内，可空）")
    private Long symmetryPairRuleId;

}
