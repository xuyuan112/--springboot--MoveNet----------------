package com.rehab.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 备份恢复请求（POST /admin/backups/{id}/restore）。
 *
 * <p>高危操作：confirmToken 为一次性令牌（先调备份详情接口获取，TTL 5 分钟），
 * reason 必填，二者缺一不可，服务端二次校验并全程审计。</p>
 */
@Data
@Schema(description = "备份恢复请求（二次确认）")
public class BackupRestoreRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 一次性确认令牌（由备份详情接口下发） */
    @Schema(description = "一次性确认令牌（先调 GET /admin/backups/{id} 获取）")
    @NotBlank(message = "确认令牌不能为空，请先获取确认码")
    private String confirmToken;

    /** 恢复原因（审计留痕） */
    @Schema(description = "恢复原因（必填，审计留痕）")
    @NotBlank(message = "恢复原因不能为空")
    @Size(max = 500, message = "恢复原因最长 500 个字符")
    private String reason;
}
