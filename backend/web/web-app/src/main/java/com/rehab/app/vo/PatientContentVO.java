package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 患者端公共内容列表项（GET /patient/contents）：仅已发布且在有效期内可见。
 */
@Data
@Builder
@Schema(description = "患者端公共内容列表项")
public class PatientContentVO {

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

    /** 封面图预签名地址（无封面时为空） */
    @Schema(description = "封面图预签名地址")
    private String coverUrl;

    /** 生效时间 */
    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    /** 失效时间（空表示长期有效） */
    @Schema(description = "失效时间")
    private LocalDateTime validTo;

    /** 发布时间 */
    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
}
