package com.rehab.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报告审核请求（POST /therapist/reports/{reportId}/review）。
 *
 * <p>decision=APPROVE → PUBLISHED（患者端可见）；REJECT → DRAFT（comment 必填，
 * 缺失 400；状态不允许审核 422 REPORT_NOT_REVIEWABLE）。</p>
 */
@Data
@Schema(description = "报告审核请求")
public class ReportReviewRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审核决定：APPROVE / REJECT */
    @Schema(description = "审核决定：APPROVE/REJECT", example = "APPROVE")
    @NotBlank(message = "审核决定不能为空")
    private String decision;

    /** 审核意见（REJECT 时必填） */
    @Schema(description = "审核意见（REJECT 时必填）", example = "结论与数据不一致，请补充达标率说明")
    @Size(max = 1000, message = "审核意见最长 1000 个字符")
    private String comment;
}
