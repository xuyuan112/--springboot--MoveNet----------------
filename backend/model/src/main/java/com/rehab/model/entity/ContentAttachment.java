package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 内容附件
 */
@Schema(description = "内容附件")
@Data
@TableName("content_attachment")
public class ContentAttachment {

    /** 附件关联ID */
    @Schema(description = "附件关联ID")
    @TableId(value = "content_attachment_id", type = IdType.AUTO)
    private Long contentAttachmentId;

    /** 内容ID */
    @Schema(description = "内容ID")
    private Long contentId;

    /** 文件ID */
    @Schema(description = "文件ID")
    private Long fileId;

    /** 附件类型：COVER封面/VIDEO视频/IMAGE图片/DOC文档 */
    @Schema(description = "附件类型：COVER封面/VIDEO视频/IMAGE图片/DOC文档")
    private String attachmentType;

    /** 排序号 */
    @Schema(description = "排序号")
    private Integer sortNo;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
