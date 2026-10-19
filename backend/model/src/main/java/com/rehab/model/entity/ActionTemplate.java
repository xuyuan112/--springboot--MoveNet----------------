package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准动作模板
 */
@Schema(description = "标准动作模板")
@Data
@TableName("action_template")
public class ActionTemplate {

    /** 模板ID */
    @Schema(description = "模板ID")
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    /** 动作版本ID */
    @Schema(description = "动作版本ID")
    private Long actionVersionId;

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

    /** 标准特征序列，JSON 字符串 */
    @Schema(description = "标准特征序列，JSON 字符串")
    private String featureSequence;

    /** 关键帧索引，JSON 字符串 */
    @Schema(description = "关键帧索引，JSON 字符串")
    private String keyframeIndex;

    /** 采样样本数 */
    @Schema(description = "采样样本数")
    private Integer sampleCount;

    /** 状态：ACTIVE/DEPRECATED */
    @Schema(description = "状态：ACTIVE/DEPRECATED")
    private String status;

    /** 创建人用户ID */
    @Schema(description = "创建人用户ID")
    private Long createdBy;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
