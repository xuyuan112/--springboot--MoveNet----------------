package com.rehab.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 咨询消息发送请求（患者端仅文字；治疗师可携带已审核资料 fileId）
 */
@Data
@Schema(description = "咨询消息发送请求（患者端仅文字；治疗师可携带已审核资料 fileId）")
public class MessageSendRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息文本内容（患者端必填且仅可发送文字） */
    @Schema(description = "消息文本内容（患者端必填且仅可发送文字）", example = "膝关节训练后有轻微酸胀，请问需要暂停吗？")
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容最长 2000 个字符")
    private String content;

    /** 附件文件ID（仅治疗师端可传，且必须为已审核资料；患者端不传） */
    @Schema(description = "附件文件ID（仅治疗师端可传，患者端不传）", example = "51")
    private Long fileId;

}
