package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 - 动作规则 VO
 */
@Data
@Builder
@Schema(description = "管理端-动作评估规则")
public class AdminActionRuleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "关节名称")
    private String jointName;

    @Schema(description = "关键点索引 JSON")
    private String keypointIndices;

    @Schema(description = "角度计算公式")
    private String angleFormula;

    @Schema(description = "关节角色：PRIMARY/AUXILIARY")
    private String jointRole;

    @Schema(description = "评估维度：ANGLE/STABILITY/SYMMETRY/HOLD/SPEED")
    private String dimension;

    @Schema(description = "动作阶段：START/CONCENTRIC/ECCENTRIC/HOLD/FULL")
    private String phase;

    @Schema(description = "最小角度")
    private BigDecimal minAngle;

    @Schema(description = "最大角度")
    private BigDecimal maxAngle;

    @Schema(description = "标准角度")
    private BigDecimal standardAngle;

    @Schema(description = "软容差（提示级）")
    private BigDecimal toleranceSoft;

    @Schema(description = "硬容差（纠错级）")
    private BigDecimal toleranceHard;

    @Schema(description = "保持所需帧数")
    private Integer holdFramesRequired;

    @Schema(description = "最小速度(度/秒)")
    private BigDecimal speedMin;

    @Schema(description = "最大速度(度/秒)")
    private BigDecimal speedMax;

    @Schema(description = "评分权重(0-1)")
    private BigDecimal scoreWeight;

    @Schema(description = "严重等级")
    private Integer severity;

    @Schema(description = "纠错提示文案")
    private String promptText;

    @Schema(description = "对称评估配对规则ID")
    private Long symmetryPairRuleId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
