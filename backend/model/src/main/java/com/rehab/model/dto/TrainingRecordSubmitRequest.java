package com.rehab.model.dto;

import com.rehab.model.enums.PoseQuality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 训练汇总提交请求（POST /patient/training-records，clientRequestId 幂等）。
 *
 * <p>服务端五重归属校验（患者/计划/计划项/每日任务/动作版本）+ 指标范围校验（422 METRIC_INVALID）
 * + 引擎一致性校验（422 ENGINE_VERSION_MISMATCH）；有效帧占比不足时记录落库为
 * INSUFFICIENT 且不计入任务进度（P4-B2）。</p>
 */
@Data
@Schema(description = "训练汇总提交请求（POST /patient/training-records，clientRequestId 幂等）")
public class TrainingRecordSubmitRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 客户端请求唯一标识（幂等防重，相同值返回首次结果） */
    @Schema(description = "客户端请求唯一标识（幂等防重，相同值返回首次结果）", example = "web-20260914-abc")
    @NotBlank(message = "clientRequestId 不能为空")
    @Size(max = 64, message = "clientRequestId 最长 64 个字符")
    private String clientRequestId;

    /** 每日任务ID */
    @Schema(description = "每日任务ID", example = "101")
    @NotNull(message = "dailyTaskId 不能为空")
    private Long dailyTaskId;

    /** 计划动作项ID（须与每日任务的计划项一致） */
    @Schema(description = "计划动作项ID（须与每日任务的计划项一致）", example = "22")
    @NotNull(message = "planItemId 不能为空")
    private Long planItemId;

    /** 动作版本ID（须与计划项固化的动作版本一致） */
    @Schema(description = "动作版本ID（须与计划项固化的动作版本一致）", example = "7")
    @NotNull(message = "actionVersionId 不能为空")
    private Long actionVersionId;

    /** 训练开始时间（ISO-8601） */
    @Schema(description = "训练开始时间（ISO-8601）", example = "2026-09-14T08:30:00")
    @NotNull(message = "startedAt 不能为空")
    private LocalDateTime startedAt;

    /** 训练结束时间（ISO-8601，需不早于开始时间） */
    @Schema(description = "训练结束时间（ISO-8601，需不早于开始时间）", example = "2026-09-14T08:35:12")
    @NotNull(message = "endedAt 不能为空")
    private LocalDateTime endedAt;

    /** 总次数 */
    @Schema(description = "总次数", example = "10")
    @NotNull(message = "totalReps 不能为空")
    @Min(value = 0, message = "总次数不能为负")
    @Max(value = 100000, message = "总次数超出允许范围")
    private Integer totalReps;

    /** 标准（正确）次数，不大于总次数 */
    @Schema(description = "标准（正确）次数，不大于总次数", example = "8")
    @NotNull(message = "correctReps 不能为空")
    @Min(value = 0, message = "正确次数不能为负")
    @Max(value = 100000, message = "正确次数超出允许范围")
    private Integer correctReps;

    /** 综合得分，范围 0~100 */
    @Schema(description = "综合得分，范围 0~100", example = "82.5")
    @NotNull(message = "totalScore 不能为空")
    @DecimalMin(value = "0", message = "综合得分需在 0~100 之间")
    @DecimalMax(value = "100", message = "综合得分需在 0~100 之间")
    private BigDecimal totalScore;

    /** 各维度评分明细，key 为维度（amplitude/hold/rhythm/stability/symmetry/similarity），value 为 0~100 分值 */
    @Schema(description = "各维度评分明细，key 为维度，value 为 0~100 分值",
            example = "{\"amplitude\":85,\"hold\":80,\"rhythm\":78}")
    @NotNull(message = "scoreDetail 不能为空")
    private Map<String, BigDecimal> scoreDetail;

    /** 姿态质量等级（GOOD/FAIR/POOR/INSUFFICIENT） */
    @Schema(description = "姿态质量等级", example = "GOOD")
    @NotNull(message = "poseQuality 不能为空")
    private PoseQuality poseQuality;

    /** 有效帧占比，范围 0~1 */
    @Schema(description = "有效帧占比，范围 0~1", example = "0.91")
    @NotNull(message = "validFrameRatio 不能为空")
    @DecimalMin(value = "0", message = "有效帧占比需在 0~1 之间")
    @DecimalMax(value = "1", message = "有效帧占比需在 0~1 之间")
    private BigDecimal validFrameRatio;

    /** 关键点修复帧占比，范围 0~1 */
    @Schema(description = "关键点修复帧占比，范围 0~1", example = "0.08")
    @DecimalMin(value = "0", message = "修复帧占比需在 0~1 之间")
    @DecimalMax(value = "1", message = "修复帧占比需在 0~1 之间")
    private BigDecimal poseRepairedRatio;

    /** 平均关节活动度(度) */
    @Schema(description = "平均关节活动度(度)", example = "88.6")
    @DecimalMin(value = "0", message = "平均活动度需不小于 0")
    @DecimalMax(value = "360", message = "平均活动度需不大于 360")
    private BigDecimal avgRom;

    /** 峰值角度(度) */
    @Schema(description = "峰值角度(度)", example = "135.2")
    @DecimalMin(value = "-360", message = "峰值角度超出允许范围")
    @DecimalMax(value = "360", message = "峰值角度超出允许范围")
    private BigDecimal peakAngle;

    /** 谷值角度(度) */
    @Schema(description = "谷值角度(度)", example = "45.0")
    @DecimalMin(value = "-360", message = "谷值角度超出允许范围")
    @DecimalMax(value = "360", message = "谷值角度超出允许范围")
    private BigDecimal troughAngle;

    /** 峰值帧号 */
    @Schema(description = "峰值帧号", example = "356")
    @Min(value = 0, message = "峰值帧号不能为负")
    private Integer peakFrameNo;

    /** 谷值帧号 */
    @Schema(description = "谷值帧号", example = "12")
    @Min(value = 0, message = "谷值帧号不能为负")
    private Integer troughFrameNo;

    /** 保持达标率，范围 0~1 */
    @Schema(description = "保持达标率，范围 0~1", example = "0.86")
    @DecimalMin(value = "0", message = "保持达标率需在 0~1 之间")
    @DecimalMax(value = "1", message = "保持达标率需在 0~1 之间")
    private BigDecimal holdRatio;

    /** 与标准模板相似度，范围 0~1 */
    @Schema(description = "与标准模板相似度，范围 0~1", example = "0.79")
    @DecimalMin(value = "0", message = "模板相似度需在 0~1 之间")
    @DecimalMax(value = "1", message = "模板相似度需在 0~1 之间")
    private BigDecimal templateSimilarity;

    /** 稳定性得分(0-100) */
    @Schema(description = "稳定性得分(0-100)", example = "84.0")
    @DecimalMin(value = "0", message = "稳定性得分需在 0~100 之间")
    @DecimalMax(value = "100", message = "稳定性得分需在 0~100 之间")
    private BigDecimal stabilityScore;

    /** 对称性得分(0-100) */
    @Schema(description = "对称性得分(0-100)", example = "86.0")
    @DecimalMin(value = "0", message = "对称性得分需在 0~100 之间")
    @DecimalMax(value = "100", message = "对称性得分需在 0~100 之间")
    private BigDecimal symmetryScore;

    /** 备注 */
    @Schema(description = "备注")
    @Size(max = 500, message = "备注最长 500 个字符")
    private String remark;

    /** 纠错摘要明细（按“规则×连续区间”聚合的纠错段，与端侧纠错队列输出一致；落库至 training_error_log） */
    @Schema(description = "纠错摘要明细（按规则×连续区间聚合）")
    @Valid
    @Size(max = 200, message = "纠错明细条数超出限制")
    private List<ErrorLogItem> errorSummary;

    /** 姿态引擎标识（需与动作版本一致） */
    @Schema(description = "姿态引擎标识（需与动作版本一致）", example = "movenet-lightning-web@1ad4f8d6c2f7-ortweb1.30.0")
    @NotBlank(message = "poseEngineId 不能为空")
    @Size(max = 150, message = "poseEngineId 最长 150 个字符")
    private String poseEngineId;

    /**
     * 纠错摘要明细项（对应端侧纠错队列按“规则×连续区间”聚合的一段纠错，
     * 落库到 training_error_log 一行）。
     */
    @Data
    @Schema(description = "纠错摘要明细项")
    public static class ErrorLogItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 触发的规则ID（非规则类纠错为空） */
        @Schema(description = "触发的规则ID（非规则类纠错为空）", example = "5")
        private Long ruleId;

        /** 评估维度（ANGLE/STABILITY/SYMMETRY/HOLD/SPEED/ACTION_MISMATCH 等，最长 20） */
        @Schema(description = "评估维度", example = "AMPLITUDE")
        @NotBlank(message = "纠错维度不能为空")
        @Size(max = 20, message = "纠错维度最长 20 个字符")
        private String dimension;

        /** 动作阶段（READY/RAISING/HOLD/LOWERING/SESSION） */
        @Schema(description = "动作阶段", example = "RAISING")
        @NotBlank(message = "动作阶段不能为空")
        @Size(max = 20, message = "动作阶段最长 20 个字符")
        private String phase;

        /** 关节名称 */
        @Schema(description = "关节名称", example = "left_elbow")
        @Size(max = 50, message = "关节名称最长 50 个字符")
        private String jointName;

        /** 偏差值（该区间内最大归一化偏差，度） */
        @Schema(description = "偏差值（区间内最大偏差）", example = "18.5")
        @DecimalMin(value = "0", message = "偏差值需不小于 0")
        private BigDecimal deviation;

        /** 严重等级（1=轻微/黄，2=明显/红） */
        @Schema(description = "严重等级（1=轻微，2=明显）", example = "1")
        @NotNull(message = "严重等级不能为空")
        @Min(value = 1, message = "严重等级为 1 或 2")
        @Max(value = 2, message = "严重等级为 1 或 2")
        private Integer severity;

        /** 发生帧号（区间起始帧） */
        @Schema(description = "发生帧号（区间起始帧）", example = "120")
        @Min(value = 0, message = "帧号不能为负")
        private Integer frameNo;

        /** 纠错提示文案（快照） */
        @Schema(description = "纠错提示文案（快照）")
        @NotBlank(message = "纠错提示文案不能为空")
        @Size(max = 255, message = "纠错提示文案最长 255 个字符")
        private String promptText;

        /** 相对训练开始时间的偏移秒数（用于还原错误发生时间，缺省 0） */
        @Schema(description = "相对训练开始时间的偏移秒数", example = "12.5")
        @DecimalMin(value = "0", message = "偏移秒数需不小于 0")
        private BigDecimal offsetSec;
    }
}
