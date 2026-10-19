package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者评估报告列表项（P5-B2）。
 *
 * <p>患者仅可见 PUBLISHED（接口文档中的 EFFECTIVE）报告；
 * DRAFT/AI_GENERATED/REVIEWED 一律 404，不泄露未审核内容。</p>
 */
@Data
@Schema(description = "患者评估报告列表项")
public class PatientReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "报告ID")
    private Long reportId;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "计划版本号")
    private Integer planVersion;

    @Schema(description = "统计周期开始")
    private LocalDate periodStart;

    @Schema(description = "统计周期结束")
    private LocalDate periodEnd;

    @Schema(description = "状态（患者侧固定 PUBLISHED）")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "生成时间")
    private LocalDateTime createdAt;
}
