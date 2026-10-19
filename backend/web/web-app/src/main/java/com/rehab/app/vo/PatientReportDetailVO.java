package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 患者评估报告详情（P5-B2，接口文档 3.4）。
 *
 * <p>含来源 planId/planVersion/period、任务完成数、训练记录数、分项评分、
 * 姿态质量统计、治疗师审核意见与免责声明；患者只读，不可修改报告。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "患者评估报告详情")
public class PatientReportDetailVO extends PatientReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "已生成任务数")
    private Integer totalTasks;

    @Schema(description = "已完成任务数")
    private Integer completedTasks;

    @Schema(description = "任务完成百分比")
    private BigDecimal completionRate;

    @Schema(description = "训练记录数（含 INSUFFICIENT）")
    private Integer recordCount;

    @Schema(description = "无效数据记录数（INSUFFICIENT，不计分）")
    private Integer insufficientCount;

    @Schema(description = "总次数合计（排除 INSUFFICIENT）")
    private Integer totalReps;

    @Schema(description = "有效达标次数合计（排除 INSUFFICIENT）")
    private Integer correctReps;

    @Schema(description = "平均总分")
    private BigDecimal avgTotalScore;

    @Schema(description = "分项评分均值（amplitude/hold/rhythm/stability/symmetry/similarity）")
    private Map<String, BigDecimal> scoreAverages;

    @Schema(description = "姿态质量分布（GOOD/FAIR/POOR/INSUFFICIENT -> 条数）")
    private Map<String, Long> qualityDistribution;

    @Schema(description = "纠错维度分布（维度 -> 次数）")
    private Map<String, Long> errorDimensionDistribution;

    @Schema(description = "治疗师审核意见")
    private String therapistConclusion;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;

    @Schema(description = "免责声明（固定文案）")
    private String disclaimer;
}
