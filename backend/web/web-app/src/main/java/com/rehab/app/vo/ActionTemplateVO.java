package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 标准动作模板 VO（任务快照下发用）。
 */
@Schema(description = "标准动作模板")
@Data
public class ActionTemplateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模板ID */
    @Schema(description = "模板ID")
    private Long templateId;

    /** 模板来源：THERAPIST治疗师录制/ALGO算法生成 */
    @Schema(description = "模板来源：THERAPIST治疗师录制/ALGO算法生成")
    private String sourceType;

    /** 采样帧率 */
    @Schema(description = "采样帧率")
    private BigDecimal frameRate;

    /** 帧数 */
    @Schema(description = "帧数")
    private Integer frameCount;

    /** 特征维度 */
    @Schema(description = "特征维度")
    private Integer featureDim;

    /** 标准特征序列 JSON */
    @Schema(description = "标准特征序列 JSON")
    private String featureSequence;

    /** 关键帧索引 JSON */
    @Schema(description = "关键帧索引 JSON")
    private String keyframeIndex;

    /** 采样样本数 */
    @Schema(description = "采样样本数")
    private Integer sampleCount;

    /** 状态：ACTIVE/DEPRECATED */
    @Schema(description = "状态：ACTIVE/DEPRECATED")
    private String status;
}
