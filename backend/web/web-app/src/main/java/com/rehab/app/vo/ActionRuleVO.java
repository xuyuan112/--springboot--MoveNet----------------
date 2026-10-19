package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 动作评估规则 VO（任务快照下发用）。
 */
@Schema(description = "动作评估规则")
@Data
public class ActionRuleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    @Schema(description = "规则ID")
    private Long actionRuleId;

    /** 关节名称 */
    @Schema(description = "关节名称")
    private String jointName;

    /** 参与计算的关键点索引 JSON */
    @Schema(description = "参与计算的关键点索引 JSON")
    private String keypointIndices;

    /** 角度计算公式 */
    @Schema(description = "角度计算公式")
    private String angleFormula;

    /** 关节角色：PRIMARY主评估/AUXILIARY辅助 */
    @Schema(description = "关节角色：PRIMARY主评估/AUXILIARY辅助")
    private String jointRole;

    /** 评估维度：ANGLE角度/STABILITY稳定/SYMMETRY对称/HOLD保持/SPEED速度 */
    @Schema(description = "评估维度：ANGLE角度/STABILITY稳定/SYMMETRY对称/HOLD保持/SPEED速度")
    private String dimension;

    /** 动作阶段：START准备/CONCENTRIC向心/ECCENTRIC离心/HOLD保持/FULL全程 */
    @Schema(description = "动作阶段：START准备/CONCENTRIC向心/ECCENTRIC离心/HOLD保持/FULL全程")
    private String phase;

    /** 最小角度 */
    @Schema(description = "最小角度")
    private BigDecimal minAngle;

    /** 最大角度 */
    @Schema(description = "最大角度")
    private BigDecimal maxAngle;

    /** 标准角度 */
    @Schema(description = "标准角度")
    private BigDecimal standardAngle;

    /** 软容差（提示级） */
    @Schema(description = "软容差（提示级）")
    private BigDecimal toleranceSoft;

    /** 硬容差（纠错级） */
    @Schema(description = "硬容差（纠错级）")
    private BigDecimal toleranceHard;

    /** 保持所需帧数 */
    @Schema(description = "保持所需帧数")
    private Integer holdFramesRequired;

    /** 最小速度(度/秒) */
    @Schema(description = "最小速度(度/秒)")
    private BigDecimal speedMin;

    /** 最大速度(度/秒) */
    @Schema(description = "最大速度(度/秒)")
    private BigDecimal speedMax;

    /** 评分权重(0-1) */
    @Schema(description = "评分权重(0-1)")
    private BigDecimal scoreWeight;

    /** 严重等级 */
    @Schema(description = "严重等级")
    private Integer severity;

    /** 纠错提示文案 */
    @Schema(description = "纠错提示文案")
    private String promptText;

    /** 对称评估配对规则ID */
    @Schema(description = "对称评估配对规则ID")
    private Long symmetryPairRuleId;
}
