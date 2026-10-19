package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公共内容列表项（GET /admin/contents）。
 */
@Data
@Builder
@Schema(description = "公共内容列表项")
public class AdminContentVO {

    /** 内容ID */
    @Schema(description = "内容ID")
    private Long contentId;

    /** 标题 */
    @Schema(description = "标题")
    private String title;

    /** 内容类型码：ARTICLE / VIDEO / GUIDE */
    @Schema(description = "内容类型码")
    private String contentType;

    /** 内容类型中文名 */
    @Schema(description = "内容类型中文名")
    private String contentTypeName;

    /** 关联康复部位编码 */
    @Schema(description = "关联康复部位编码")
    private String targetPartCode;

    /** 关联康复部位名称 */
    @Schema(description = "关联康复部位名称")
    private String targetPartName;

    /** 目标人群标签 */
    @Schema(description = "目标人群标签")
    private String targetGroup;

    /** 摘要 */
    @Schema(description = "摘要")
    private String summary;

    /** 状态码：DRAFT / PENDING_REVIEW / PUBLISHED / OFFLINE */
    @Schema(description = "状态码")
    private String status;

    /** 状态中文名 */
    @Schema(description = "状态中文名")
    private String statusName;

    /** 是否存在最近一次审核意见（DRAFT 态下用于前端展示「已驳回」标记） */
    @Schema(description = "是否存在审核/驳回意见")
    private Boolean hasReviewRemark;

    /** 最近审核意见 */
    @Schema(description = "最近审核意见")
    private String reviewRemark;

    /** 生效时间 */
    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    /** 失效时间（空表示长期有效） */
    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    /** 发布时间 */
    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    /** 作者用户ID */
    @Schema(description = "作者用户ID")
    private Long authorId;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
