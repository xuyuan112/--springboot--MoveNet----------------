package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 治疗师端纠错日志行（P7-B1）：维度/阶段/严重度/偏差/帧号。
 */
@Data
@Schema(description = "治疗师端纠错日志行")
public class TherapistErrorLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "纠错日志ID")
    private Long errorId;

    @Schema(description = "评估维度")
    private String dimension;

    @Schema(description = "动作阶段")
    private String phase;

    @Schema(description = "错误类型：AMPLITUDE不足/SPEED过快等")
    private String errorType;

    @Schema(description = "关节名称")
    private String jointName;

    @Schema(description = "实测值")
    private BigDecimal currentValue;

    @Schema(description = "期望值")
    private BigDecimal expectedValue;

    @Schema(description = "偏差值")
    private BigDecimal deviation;

    @Schema(description = "严重等级")
    private Integer severity;

    @Schema(description = "发生帧号")
    private Integer frameNo;

    @Schema(description = "纠错提示文案（快照）")
    private String promptText;

    @Schema(description = "错误发生时间")
    private LocalDateTime errorAt;
}
