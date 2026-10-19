package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 评估报告
 */
@Schema(description = "评估报告")
@Data
@TableName("assessment_report")
public class AssessmentReport {

    /** 报告ID */
    @Schema(description = "报告ID")
    @TableId(value = "report_id", type = IdType.AUTO)
    private Long reportId;

    /** 计划ID */
    @Schema(description = "计划ID")
    private Long planId;

    /** 统计周期开始 */
    @Schema(description = "统计周期开始")
    private LocalDate periodStart;

    /** 统计周期结束 */
    @Schema(description = "统计周期结束")
    private LocalDate periodEnd;

    /** 报告版本号（同计划同周期内从1递增，regenerate 产生新版本） */
    @Schema(description = "报告版本号（同计划同周期内从1递增）")
    private Integer versionNo;

    /** 报告数据快照（指标聚合结果），JSON 字符串 */
    @Schema(description = "报告数据快照（指标聚合结果），JSON 字符串")
    private String dataSnapshot;

    /** AI生成的报告草稿 */
    @Schema(description = "AI生成的报告草稿")
    private String aiDraft;

    /** 治疗师审核结论 */
    @Schema(description = "治疗师审核结论")
    private String therapistConclusion;

    /** 状态：DRAFT/AI_GENERATED/REVIEWED/PUBLISHED */
    @Schema(description = "状态：DRAFT/AI_GENERATED/REVIEWED/PUBLISHED")
    private String status;

    /** 审核治疗师用户ID */
    @Schema(description = "审核治疗师用户ID")
    private Long reviewedBy;

    /** 审核时间 */
    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
