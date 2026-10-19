package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 治疗师端评估报告详情（P7-B4）：正文 + data_snapshot 统计 + AI 草稿 + 结论 + 免责声明。
 */
@Data
@Schema(description = "治疗师端评估报告详情")
public class TherapistReportDetailVO implements Serializable {

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

    @Schema(description = "数据快照（指标聚合结果，JSON 对象）")
    private Map<String, Object> dataSnapshot;

    @Schema(description = "AI 生成的小结草稿（可选字段，仅草稿参考）")
    private String aiDraft;

    @Schema(description = "治疗师结论")
    private String therapistConclusion;

    @Schema(description = "审核治疗师姓名")
    private String reviewerName;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "医疗免责声明（常驻）")
    private String disclaimer;
}
