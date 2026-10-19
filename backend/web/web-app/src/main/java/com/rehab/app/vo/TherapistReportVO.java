package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 治疗师端评估报告列表行（P7-B4）。
 */
@Data
@Schema(description = "治疗师端评估报告列表行")
public class TherapistReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "报告ID")
    private Long reportId;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "患者用户ID")
    private Long patientId;

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "统计周期开始")
    private LocalDate periodStart;

    @Schema(description = "统计周期结束")
    private LocalDate periodEnd;

    @Schema(description = "报告版本号")
    private Integer versionNo;

    @Schema(description = "状态：DRAFT/AI_GENERATED/REVIEWED/PUBLISHED")
    private String status;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
