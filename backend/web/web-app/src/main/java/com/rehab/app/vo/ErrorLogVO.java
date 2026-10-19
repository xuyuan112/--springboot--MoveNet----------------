package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 训练纠错日志 VO（来自 training_error_log，不含原始关键点）。
 */
@Schema(description = "训练纠错日志")
@Data
public class ErrorLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 纠错日志ID */
    @Schema(description = "纠错日志ID")
    private Long errorId;

    /** 触发的规则ID（NULL为非规则类错误） */
    @Schema(description = "触发的规则ID（NULL为非规则类错误）")
    private Long ruleId;

    /** 评估维度 */
    @Schema(description = "评估维度")
    private String dimension;

    /** 动作阶段 */
    @Schema(description = "动作阶段")
    private String phase;

    /** 错误类型 */
    @Schema(description = "错误类型")
    private String errorType;

    /** 关节名称 */
    @Schema(description = "关节名称")
    private String jointName;

    /** 实测值 */
    @Schema(description = "实测值")
    private BigDecimal currentValue;

    /** 期望值 */
    @Schema(description = "期望值")
    private BigDecimal expectedValue;

    /** 偏差值 */
    @Schema(description = "偏差值")
    private BigDecimal deviation;

    /** 严重等级（1轻微/2明显） */
    @Schema(description = "严重等级（1轻微/2明显）")
    private Integer severity;

    /** 发生帧号 */
    @Schema(description = "发生帧号")
    private Integer frameNo;

    /** 纠错提示文案（快照） */
    @Schema(description = "纠错提示文案（快照）")
    private String promptText;

    /** 错误发生时间 */
    @Schema(description = "错误发生时间")
    private java.time.LocalDateTime errorAt;
}
