package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 咨询消息
 */
@Schema(description = "咨询消息")
@Data
@TableName("consultation_message")
public class ConsultationMessage {

    /** 消息ID */
    @Schema(description = "消息ID")
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    /** 会话ID */
    @Schema(description = "会话ID")
    private Long consultationId;

    /** 发送人用户ID */
    @Schema(description = "发送人用户ID")
    private Long senderId;

    /** 文本内容（纯附件消息可为空） */
    @Schema(description = "文本内容（纯附件消息可为空）")
    private String content;

    /** 是否AI草稿 1是0否 */
    @Schema(description = "是否AI草稿 1是0否")
    private Integer isAiDraft;

    /** 是否已读 1是0否 */
    @Schema(description = "是否已读 1是0否")
    private Integer isRead;

    /** 阅读时间 */
    @Schema(description = "阅读时间")
    private LocalDateTime readAt;

    /** 发送时间 */
    @Schema(description = "发送时间")
    private LocalDateTime createdAt;
}
