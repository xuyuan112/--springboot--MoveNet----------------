package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容附件视图（含 MinIO 预签名预览地址）。
 */
@Data
@Builder
@Schema(description = "内容附件视图")
public class ContentAttachmentVO {

    /** 附件关联ID */
    @Schema(description = "附件关联ID")
    private Long contentAttachmentId;

    /** 文件ID */
    @Schema(description = "文件ID")
    private Long fileId;

    /** 附件类型码：COVER / VIDEO / IMAGE / DOC */
    @Schema(description = "附件类型码")
    private String attachmentType;

    /** 附件类型中文名 */
    @Schema(description = "附件类型中文名")
    private String attachmentTypeName;

    /** 排序号 */
    @Schema(description = "排序号")
    private Integer sortNo;

    /** 原始文件名 */
    @Schema(description = "原始文件名")
    private String fileName;

    /** MIME 类型 */
    @Schema(description = "MIME 类型")
    private String contentType;

    /** 文件大小（字节） */
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /** 短时预签名下载地址 */
    @Schema(description = "短时预签名下载地址")
    private String url;

    /** URL 过期时间 */
    @Schema(description = "URL 过期时间")
    private LocalDateTime urlExpiresAt;
}
