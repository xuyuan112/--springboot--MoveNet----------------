package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 趋势数据点（P5-B1 /patient/trends）。
 *
 * <p>聚合排除 INSUFFICIENT 记录；granularity=DAY 按自然日、WEEK 按 ISO 周一起始。</p>
 */
@Data
@Schema(description = "训练趋势数据点")
public class PatientTrendPointVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日期（DAY=yyyy-MM-dd；WEEK=该周周一日期）")
    private String date;

    @Schema(description = "训练次数")
    private Integer recordCount;

    @Schema(description = "平均总分（0-100）")
    private BigDecimal avgScore;

    @Schema(description = "平均达标率（有效达标次数/总次数 的均值，0-1）")
    private BigDecimal avgCompletionRate;

    @Schema(description = "总次数合计")
    private Integer totalReps;

    @Schema(description = "有效达标次数合计")
    private Integer correctReps;

    // ===== 治疗师端扩展维度（P7-B2；患者端接口不填充，保持 null）=====

    @Schema(description = "平均关节活动度 ROM（度，可选维度）")
    private BigDecimal avgRom;

    @Schema(description = "平均保持达标率（0-1，可选维度）")
    private BigDecimal avgHoldRatio;

    @Schema(description = "平均有效帧占比（0-1，姿态质量指标，用于区分表现变差与拍摄条件变差）")
    private BigDecimal avgValidFrameRatio;

    @Schema(description = "平均关键点修复帧占比（0-1，姿态质量指标）")
    private BigDecimal avgPoseRepairedRatio;
}
