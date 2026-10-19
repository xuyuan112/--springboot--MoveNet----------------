package com.rehab.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 公共内容审核请求（POST /admin/contents/{contentId}/audit）
 */
@Data
@Schema(description = "公共内容审核请求（POST /admin/contents/{contentId}/audit）")
public class ContentAuditRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审核动作：APPROVE 通过 / REJECT 驳回（必填理由）/ OFFLINE 下线 */
    @Schema(description = "审核动作：APPROVE / REJECT / OFFLINE", example = "APPROVE")
    @NotBlank(message = "审核动作不能为空")
    @Pattern(regexp = "APPROVE|REJECT|OFFLINE", message = "decision 仅支持 APPROVE、REJECT、OFFLINE")
    private String decision;

    /** 审核意见（REJECT 时必填） */
    @Schema(description = "审核意见（REJECT 时必填）")
    @Size(max = 1000, message = "审核意见最长 1000 个字符")
    private String comment;

}
