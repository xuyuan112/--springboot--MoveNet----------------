package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 训练记录
 */
@Schema(description = "训练记录")
@Data
@TableName("training_record")
public class TrainingRecord {

    /** 记录ID */
    @Schema(description = "记录ID")
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    /** 每日任务ID */
    @Schema(description = "每日任务ID")
    private Long dailyTaskId;

    /** 动作版本快照（提交时从 plan_item 复制，不随后续版本变更变化） */
    @Schema(description = "动作版本快照（提交时从 plan_item 复制，不随后续版本变更变化）")
    private Long actionVersionId;

    /** 训练开始时间 */
    @Schema(description = "训练开始时间")
    private LocalDateTime startedAt;

    /** 训练结束时间 */
    @Schema(description = "训练结束时间")
    private LocalDateTime endedAt;

    /** 总次数 */
    @Schema(description = "总次数")
    private Integer totalReps;

    /** 标准次数 */
    @Schema(description = "标准次数")
    private Integer correctReps;

    /** 综合得分(0-100) */
    @Schema(description = "综合得分(0-100)")
    private BigDecimal totalScore;

    /** 评分明细（各维度得分），JSON 字符串 */
    @Schema(description = "评分明细（各维度得分），JSON 字符串")
    private String scoreDetail;

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

    /** 有效帧占比(0-1) */
    @Schema(description = "有效帧占比(0-1)")
    private BigDecimal validFrameRatio;

    /** 关键点修复帧占比(0-1) */
    @Schema(description = "关键点修复帧占比(0-1)")
    private BigDecimal poseRepairedRatio;

    /** 稳定性得分(0-100) */
    @Schema(description = "稳定性得分(0-100)")
    private BigDecimal stabilityScore;

    /** 对称性得分(0-100) */
    @Schema(description = "对称性得分(0-100)")
    private BigDecimal symmetryScore;

    /** 姿态质量：GOOD/FAIR/POOR */
    @Schema(description = "姿态质量：GOOD/FAIR/POOR")
    private String poseQuality;

    /** 姿态引擎标识（分析时快照） */
    @Schema(description = "姿态引擎标识（分析时快照）")
    private String poseEngineId;

    /** 客户端请求ID（幂等防重） */
    @Schema(description = "客户端请求ID（幂等防重）")
    private String clientRequestId;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 逻辑删除：0正常1已撤回（P4-B2 撤回为软删除，撤回原因与时间另存） */
    @Schema(description = "逻辑删除：0正常1已撤回")
    @TableLogic
    private Integer deleted;

    /** 撤回原因（撤回必填） */
    @Schema(description = "撤回原因（撤回必填）")
    private String withdrawReason;

    /** 撤回时间 */
    @Schema(description = "撤回时间")
    private LocalDateTime withdrawnAt;
}
