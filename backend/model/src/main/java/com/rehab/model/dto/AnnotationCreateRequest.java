package com.rehab.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治疗师训练复核批注请求（POST /therapist/training-records/{recordId}/annotations，不修改原始评分）
 */
@Data
@Schema(description = "治疗师训练复核批注请求（POST /therapist/training-records/{recordId}/annotations，不修改原始评分）")
public class AnnotationCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 批注内容 */
    @Schema(description = "批注内容", example = "第 3 组伸膝角度略不足，建议放慢速度")
    @NotBlank(message = "批注内容不能为空")
    @Size(max = 1000, message = "批注最长 1000 个字符")
    private String annotation;

}
