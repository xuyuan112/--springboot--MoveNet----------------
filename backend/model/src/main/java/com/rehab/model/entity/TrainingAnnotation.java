package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 训练复核批注（P7-B1）：治疗师对训练记录的复核意见，只增不改，不修改原始评分。
 */
@Schema(description = "训练复核批注")
@Data
@TableName("rehab_training_annotation")
public class TrainingAnnotation {

    /** 批注ID */
    @Schema(description = "批注ID")
    @TableId(value = "annotation_id", type = IdType.AUTO)
    private Long annotationId;

    /** 训练记录ID */
    @Schema(description = "训练记录ID")
    private Long recordId;

    /** 批注治疗师用户ID */
    @Schema(description = "批注治疗师用户ID")
    private Long therapistId;

    /** 批注内容 */
    @Schema(description = "批注内容")
    private String annotation;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
