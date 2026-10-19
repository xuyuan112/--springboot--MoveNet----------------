package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 患者收藏列表项（P5-B3）。
 *
 * <p>内容下架/过期后历史收藏保留，但 available=false 且不再提供封面与正文入口。</p>
 */
@Data
@Schema(description = "患者收藏列表项")
public class PatientCollectionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "收藏ID")
    private Long collectionId;

    @Schema(description = "内容ID")
    private Long contentId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "内容类型名称")
    private String contentTypeName;

    @Schema(description = "适用部位编码")
    private String targetPartCode;

    @Schema(description = "适用部位名称")
    private String targetPartName;

    @Schema(description = "摘要（不可用时为空）")
    private String summary;

    @Schema(description = "封面预签名地址（不可用时为空）")
    private String coverUrl;

    @Schema(description = "当前是否可查看正文")
    private Boolean available;

    @Schema(description = "收藏时间")
    private LocalDateTime collectedAt;
}
