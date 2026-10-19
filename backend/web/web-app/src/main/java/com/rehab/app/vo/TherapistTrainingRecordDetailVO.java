package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 治疗师端训练记录详情（P7-B1）：含分维度评分构成与算法版本快照；
 * 不含原始视频与逐帧关键点。
 */
@Data
@Schema(description = "治疗师端训练记录详情")
public class TherapistTrainingRecordDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    private Long recordId;

    @Schema(description = "患者用户ID")
    private Long patientId;

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "每日任务ID")
    private Long dailyTaskId;

    @Schema(description = "任务日期")
    private LocalDate taskDate;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "动作名称")
    private String actionName;

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

    @Schema(description = "分维度评分构成（评分明细 JSON 解析）")
    private Map<String, BigDecimal> scoreDetail;

    @Schema(description = "平均关节活动度(度)")
    private BigDecimal avgRom;

    @Schema(description = "峰值角度(度)")
    private BigDecimal peakAngle;

    @Schema(description = "谷值角度(度)")
    private BigDecimal troughAngle;

    @Schema(description = "保持达标率(0-1)")
    private BigDecimal holdRatio;

    @Schema(description = "与标准模板相似度(0-1)")
    private BigDecimal templateSimilarity;

    @Schema(description = "有效帧占比(0-1)")
    private BigDecimal validFrameRatio;

    @Schema(description = "关键点修复帧占比(0-1)")
    private BigDecimal poseRepairedRatio;

    @Schema(description = "稳定性得分(0-100)")
    private BigDecimal stabilityScore;

    @Schema(description = "对称性得分(0-100)")
    private BigDecimal symmetryScore;

    @Schema(description = "姿态质量：GOOD/FAIR/POOR/INSUFFICIENT")
    private String poseQuality;

    @Schema(description = "姿态引擎标识（分析时快照）")
    private String poseEngineId;

    @Schema(description = "备注")
    private String remark;
}
