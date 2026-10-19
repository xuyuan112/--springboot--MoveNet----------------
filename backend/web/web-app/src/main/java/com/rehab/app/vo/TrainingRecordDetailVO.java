package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 患者端训练记录详情 VO（含分项评分、关键指标与纠错摘要；不含原始视频与逐帧关键点）。
 */
@Schema(description = "患者端训练记录详情")
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingRecordDetailVO extends TrainingRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 各维度评分明细（amplitude/hold/rhythm/stability/symmetry/similarity） */
    @Schema(description = "各维度评分明细")
    private Map<String, BigDecimal> scoreDetail;

    /** 平均关节活动度(度) */
    @Schema(description = "平均关节活动度(度)")
    private BigDecimal avgRom;

    /** 峰值角度(度) */
    @Schema(description = "峰值角度(度)")
    private BigDecimal peakAngle;

    /** 谷值角度(度) */
    @Schema(description = "谷值角度(度)")
    private BigDecimal troughAngle;

    /** 峰值帧号 */
    @Schema(description = "峰值帧号")
    private Integer peakFrameNo;

    /** 谷值帧号 */
    @Schema(description = "谷值帧号")
    private Integer troughFrameNo;

    /** 保持达标率(0-1) */
    @Schema(description = "保持达标率(0-1)")
    private BigDecimal holdRatio;

    /** 与标准模板相似度(0-1) */
    @Schema(description = "与标准模板相似度(0-1)")
    private BigDecimal templateSimilarity;

    /** 稳定性得分(0-100) */
    @Schema(description = "稳定性得分(0-100)")
    private BigDecimal stabilityScore;

    /** 对称性得分(0-100) */
    @Schema(description = "对称性得分(0-100)")
    private BigDecimal symmetryScore;

    /** 姿态引擎标识（分析时快照） */
    @Schema(description = "姿态引擎标识（分析时快照）")
    private String poseEngineId;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 纠错摘要明细（按规则×连续区间聚合） */
    @Schema(description = "纠错摘要明细")
    private List<ErrorLogVO> errorLogs;

    /** 纠错维度分布（维度 → 次数） */
    @Schema(description = "纠错维度分布")
    private List<ErrorGroupVO> errorSummary;
}
