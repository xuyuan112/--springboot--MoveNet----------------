package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 - 动作模板 VO
 */
@Data
@Builder
@Schema(description = "管理端-动作标准模板")
public class AdminActionTemplateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "动作版本ID")
    private Long actionVersionId;

    @Schema(description = "模板来源：THERAPIST/ALGO")
    private String sourceType;

    @Schema(description = "模板来源名称")
    private String sourceTypeName;

    @Schema(description = "采样帧率")
    private BigDecimal frameRate;

    @Schema(description = "帧数")
    private Integer frameCount;

    @Schema(description = "特征维度")
    private Integer featureDim;

    @Schema(description = "标准特征序列，JSON 字符串")
    private String featureSequence;

    @Schema(description = "关键帧索引，JSON 字符串")
    private String keyframeIndex;

    @Schema(description = "采样样本数")
    private Integer sampleCount;

    @Schema(description = "状态：ACTIVE/DEPRECATED")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
