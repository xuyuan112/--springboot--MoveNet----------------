package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 治疗师端患者列表项 VO（GET /therapist/patients）。
 * 手机号脱敏；风险提示仅在列表标记，明细见患者详情接口。
 */
@Data
@Schema(description = "治疗师端患者列表项")
public class TherapistPatientVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "患者用户ID")
    private Long patientId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别：M男/F女/U未知")
    private String gender;

    @Schema(description = "出生日期")
    private LocalDate birthday;

    @Schema(description = "手机号（脱敏）")
    private String phone;

    @Schema(description = "主康复部位名称")
    private String rehabPartName;

    @Schema(description = "绑定关系类型：PRIMARY主治/SECONDARY协作")
    private String relationType;

    @Schema(description = "绑定开始时间")
    private LocalDateTime bindingStartAt;

    @Schema(description = "进行中（PUBLISHED/PAUSED）计划数")
    private Long activePlanCount;

    @Schema(description = "最近一次计划状态（无计划为 null）")
    private String latestPlanStatus;

    @Schema(description = "最近一次计划状态名称")
    private String latestPlanStatusName;

    @Schema(description = "近 7 日任务完成率（0-100，无任务为 null）")
    private Double recentCompletionRate;

    @Schema(description = "是否存在风险提示（仅提示，不构成医学判断）")
    private Boolean hasRiskHint;
}
