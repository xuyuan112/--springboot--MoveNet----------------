package com.rehab.model.dto;

import com.rehab.model.enums.AttachmentType;
import com.rehab.model.enums.ContentType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 公共健康内容创建/修改请求（POST、PUT /admin/contents[/{contentId}]）
 */
@Data
@Schema(description = "公共健康内容创建/修改请求（POST、PUT /admin/contents[/{contentId}]）")
public class HealthContentSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标题 */
    @Schema(description = "标题", example = "膝关节术后居家训练指南")
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 个字符")
    private String title;

    /** 内容类型：ARTICLE 图文 / VIDEO 视频 / GUIDE 指南 */
    @Schema(description = "内容类型：ARTICLE / VIDEO / GUIDE", example = "ARTICLE")
    @NotNull(message = "内容类型不能为空")
    private ContentType contentType;

    /** 关联康复部位编码 */
    @Schema(description = "关联康复部位编码", example = "KNEE")
    @Size(max = 30)
    private String targetPartCode;

    /** 目标人群标签 */
    @Schema(description = "目标人群标签")
    @Size(max = 200)
    private String targetGroup;

    /** 摘要 */
    @Schema(description = "摘要")
    @Size(max = 500)
    private String summary;

    /** 正文 */
    @Schema(description = "正文")
    @NotBlank(message = "正文不能为空")
    @Size(max = 10000, message = "正文最长 10000 个字符")
    private String contentBody;

    /** 生效时间（为空则发布时生效） */
    @Schema(description = "生效时间，格式 yyyy-MM-dd HH:mm:ss（为空则发布时生效）", example = "2026-10-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    /** 失效时间（为空表示长期有效） */
    @Schema(description = "失效时间，格式 yyyy-MM-dd HH:mm:ss（为空表示长期有效）", example = "2026-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validTo;

    /** 附件列表（封面/视频/图片/文档，需已通过文件上传确认） */
    @Schema(description = "附件列表")
    @Valid
    private List<AttachmentPayload> attachments;
    /**
     * 内容附件载荷
     */
    @Data
    @Schema(description = "内容附件载荷")
    public static class AttachmentPayload implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 文件ID（file_object） */
        @Schema(description = "文件ID", example = "301")
        @NotNull(message = "附件文件ID不能为空")
        private Long fileId;

        /** 附件类型：COVER 封面 / VIDEO 视频 / IMAGE 图片 / DOC 文档 */
        @Schema(description = "附件类型：COVER / VIDEO / IMAGE / DOC", example = "COVER")
        @NotNull(message = "附件类型不能为空")
        private AttachmentType attachmentType;

        /** 排序号 */
        @Schema(description = "排序号", example = "1")
        @Min(value = 0, message = "排序号不能为负")
        private Integer sortNo;
    }
}
