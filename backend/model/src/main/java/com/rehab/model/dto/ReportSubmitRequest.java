package com.rehab.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报告提交请求（POST /therapist/reports/{reportId}/submit，DRAFT/AI_GENERATED→REVIEWED）
 */
@Data
@Schema(description = "报告提交请求（补充治疗师结论）")
public class ReportSubmitRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 治疗师结论（必填，提交后随报告进入审核流） */
    @Schema(description = "治疗师结论", example = "本周膝关节屈伸达标率 82%，建议维持当前强度并增加 1 组/日。")
    @NotBlank(message = "治疗师结论不能为空")
    @Size(max = 2000, message = "结论最长 2000 个字符")
    private String therapistConclusion;
}
