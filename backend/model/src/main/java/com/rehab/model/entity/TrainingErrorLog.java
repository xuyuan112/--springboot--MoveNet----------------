package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 训练纠错日志
 */
@Schema(description = "训练纠错日志")
@Data
@TableName("training_error_log")
public class TrainingErrorLog {

    /** 纠错日志ID */
    @Schema(description = "纠错日志ID")
    @TableId(value = "error_id", type = IdType.AUTO)
    private Long errorId;

    /** 训练记录ID */
    @Schema(description = "训练记录ID")
    private Long recordId;

    /** 触发的规则ID（NULL为非规则类错误） */
    @Schema(description = "触发的规则ID（NULL为非规则类错误）")
    private Long ruleId;

    /** 评估维度 */
    @Schema(description = "评估维度")
    private String dimension;

    /** 动作阶段 */
    @Schema(description = "动作阶段")
    private String phase;

    /** 错误类型：AMPLITUDE不足/SPEED过快等 */
    @Schema(description = "错误类型：AMPLITUDE不足/SPEED过快等")
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

    /** 严重等级 */
    @Schema(description = "严重等级")
    private Integer severity;

    /** 发生帧号 */
    @Schema(description = "发生帧号")
    private Integer frameNo;

    /** 纠错提示文案（快照） */
    @Schema(description = "纠错提示文案（快照）")
    private String promptText;

    /** 错误发生时间 */
    @Schema(description = "错误发生时间")
    private LocalDateTime errorAt;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
