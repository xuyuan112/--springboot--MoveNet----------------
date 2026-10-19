package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 治疗师端训练记录列表行（P7-B1）。
 */
@Data
@Schema(description = "治疗师端训练记录列表行")
public class TherapistTrainingRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    private Long recordId;

    @Schema(description = "患者用户ID")
    private Long patientId;

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "动作名称")
    private String actionName;

    @Schema(description = "任务日期")
    private LocalDate taskDate;

    @Schema(description = "训练开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "训练结束时间")
    private LocalDateTime endedAt;

    @Schema(description = "总次数")
    private Integer totalReps;

    @Schema(description = "标准次数")
    private Integer correctReps;

    @Schema(description = "综合得分(0-100)")
    private BigDecimal totalScore;

    @Schema(description = "姿态质量：GOOD/FAIR/POOR/INSUFFICIENT")
    private String poseQuality;

    @Schema(description = "有效帧占比(0-1)")
    private BigDecimal validFrameRatio;

    @Schema(description = "平均关节活动度(度)")
    private BigDecimal avgRom;

    @Schema(description = "保持达标率(0-1)")
    private BigDecimal holdRatio;
}
