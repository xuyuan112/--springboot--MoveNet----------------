package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端公告 VO（列表 / 详情通用）。
 */
@Data
@Schema(description = "管理端公告")
public class AdminAnnouncementVO {

    @Schema(description = "公告ID")
    private Long announcementId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "公告正文（详情接口返回全文）")
    private String content;

    @Schema(description = "可见范围码值：ALL/PATIENT/THERAPIST")
    private String scope;

    @Schema(description = "可见范围中文名")
    private String scopeName;

    @Schema(description = "状态码值：DRAFT/PUBLISHED/OFFLINE")
    private String status;

    @Schema(description = "状态中文名")
    private String statusName;

    @Schema(description = "展示开始时间")
    private LocalDateTime startTime;

    @Schema(description = "展示结束时间")
    private LocalDateTime endTime;

    @Schema(description = "创建人用户ID")
    private Long createdBy;

    @Schema(description = "创建人账号")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
