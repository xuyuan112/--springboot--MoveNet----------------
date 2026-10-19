package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 患者端公共内容详情：正文、适用人群与附件预签名地址；
 * 不返回审核人、审核意见、作者等管理端字段。
 */
@Data
@Builder
@Schema(description = "患者端公共内容详情")
public class PatientContentDetailVO {

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

    /** 生效时间 */
    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    /** 失效时间 */
    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    /** 发布时间 */
    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    /** 附件列表（按 sortNo 升序，含预签名地址与过期时间） */
    @Schema(description = "附件列表")
    private List<AttachmentVO> attachments;

    /**
     * 患者端附件视图（字段与接口文档 3.5 节示例对齐）。
     */
    @Data
    @Builder
    @Schema(description = "患者端内容附件")
    public static class AttachmentVO {

        /** 附件关联ID */
        @Schema(description = "附件ID")
        private Long id;

        /** 附件类型：COVER / VIDEO / IMAGE / DOC */
        @Schema(description = "附件类型")
        private String type;

        /** 附件类型中文名 */
        @Schema(description = "附件类型中文名")
        private String typeName;

        /** 文件名 */
        @Schema(description = "文件名")
        private String name;

        /** MIME 类型 */
        @Schema(description = "MIME 类型")
        private String contentType;

        /** 文件大小（字节） */
        @Schema(description = "文件大小（字节）")
        private Long size;

        /** 短时预签名下载地址 */
        @Schema(description = "短时预签名下载地址")
        private String url;

        /** URL 过期时间 */
        @Schema(description = "URL 过期时间")
        private LocalDateTime urlExpiresAt;
    }
}
