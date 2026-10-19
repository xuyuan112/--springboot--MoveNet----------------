package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者端康复计划 VO。
 */
@Schema(description = "患者端康复计划")
@Data
public class PatientPlanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 计划ID */
    @Schema(description = "计划ID")
    private Long planId;

    /** 计划名称 */
    @Schema(description = "计划名称")
    private String name;

    /** 康复部位编码 */
    @Schema(description = "康复部位编码")
    private String rehabPartCode;

    /** 康复部位名称 */
    @Schema(description = "康复部位名称")
    private String rehabPartName;

    /** 治疗师姓名 */
    @Schema(description = "治疗师姓名")
    private String therapistName;

    /** 计划版本号 */
    @Schema(description = "计划版本号")
    private Integer versionNo;

    /** 开始日期 */
    @Schema(description = "开始日期")
    private LocalDate startDate;

    /** 结束日期 */
    @Schema(description = "结束日期")
    private LocalDate endDate;

    /** 频次类型：DAILY每日/WEEKLY每周/CUSTOM自定义 */
    @Schema(description = "频次类型：DAILY每日/WEEKLY每周/CUSTOM自定义")
    private String frequencyType;

    /** 频次值（如每周N次） */
    @Schema(description = "频次值（如每周N次）")
    private Integer frequencyValue;

    /** 状态：DRAFT/PUBLISHED/PAUSED/COMPLETED/ARCHIVED */
    @Schema(description = "状态：DRAFT/PUBLISHED/PAUSED/COMPLETED/ARCHIVED")
    private String status;

    /** 状态名称 */
    @Schema(description = "状态名称")
    private String statusName;

    /** 备注/注意事项 */
    @Schema(description = "备注/注意事项")
    private String remark;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
