package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 咨询消息附件
 */
@Schema(description = "咨询消息附件")
@Data
@TableName("consultation_attachment")
public class ConsultationAttachment {

    /** 消息附件ID */
    @Schema(description = "消息附件ID")
    @TableId(value = "consultation_attachment_id", type = IdType.AUTO)
    private Long consultationAttachmentId;

    /** 消息ID */
    @Schema(description = "消息ID")
    private Long messageId;

    /** 文件ID */
    @Schema(description = "文件ID")
    private Long fileId;

    /** 排序号 */
    @Schema(description = "排序号")
    private Integer sortNo;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
