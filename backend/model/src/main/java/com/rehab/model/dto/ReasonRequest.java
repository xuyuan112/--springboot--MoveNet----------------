package com.rehab.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用操作原因请求（暂停/结束/撤回/禁用/解绑/下线/归档等不可逆或需审计的操作）
 */
@Data
@Schema(description = "通用操作原因请求（暂停/结束/撤回/禁用/解绑/下线/归档等不可逆或需审计的操作）")
public class ReasonRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 操作原因（必填，写入审计/操作日志） */
    @Schema(description = "操作原因（必填，写入审计/操作日志）", example = "训练中膝盖疼痛，暂停本周计划")
    @NotBlank(message = "原因不能为空")
    @Size(max = 200, message = "原因最长 200 个字符")
    private String reason;

}
