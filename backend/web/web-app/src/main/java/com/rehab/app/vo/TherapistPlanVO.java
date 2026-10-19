package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 治疗师端康复计划 VO（列表行与版本行共用基础字段）。
 */
@Data
@Schema(description = "治疗师端康复计划")
public class TherapistPlanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "患者用户ID")
    private Long patientId;

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "计划名称")
    private String name;

    @Schema(description = "康复部位编码")
    private String rehabPartCode;

    @Schema(description = "康复部位名称")
    private String rehabPartName;

    @Schema(description = "计划版本号（同名计划链内递增）")
    private Integer versionNo;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "频次类型：DAILY/WEEKLY/CUSTOM")
    private String frequencyType;

    @Schema(description = "频次值")
    private Integer frequencyValue;

    @Schema(description = "状态：DRAFT/PUBLISHED/PAUSED/COMPLETED/ARCHIVED")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "备注/注意事项")
    private String remark;

    @Schema(description = "动作项数量")
    private Long itemCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
