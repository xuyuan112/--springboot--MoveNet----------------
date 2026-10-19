package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公共内容详情（GET /admin/contents/{contentId}）：正文 + 审核信息 + 附件预签名地址。
 */
@Data
@Builder
@Schema(description = "公共内容详情")
public class AdminContentDetailVO {

    /** 内容ID */
    @Schema(description = "内容ID")
    private Long contentId;

    /** 标题 */
    @Schema(description = "标题")
    private String title;

    /** 内容类型码 */
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

    /** 正文 */
    @Schema(description = "正文")
    private String contentBody;

    /** 状态码 */
    @Schema(description = "状态码")
    private String status;

    /** 状态中文名 */
    @Schema(description = "状态中文名")
    private String statusName;

    /** 作者用户ID */
    @Schema(description = "作者用户ID")
    private Long authorId;

    /** 审核人用户ID */
    @Schema(description = "审核人用户ID")
    private Long reviewerId;

    /** 审核意见（驳回理由 / 下架原因） */
    @Schema(description = "审核意见")
    private String reviewRemark;

    /** 生效时间 */
    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    /** 失效时间 */
    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    /** 发布时间 */
    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /** 附件列表（按 sortNo 升序，含预签名预览地址） */
    @Schema(description = "附件列表")
    private List<ContentAttachmentVO> attachments;
}
