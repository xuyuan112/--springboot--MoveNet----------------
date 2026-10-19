package com.rehab.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分片/预签名上传完成确认请求（POST .../files/{uploadId}/complete，校验大小、类型、SHA-256）
 */
@Data
@Schema(description = "分片/预签名上传完成确认请求（POST .../files/{uploadId}/complete，校验大小、类型、SHA-256）")
public class FileCompleteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件 SHA-256 摘要（64 位小写十六进制），用于与实际上传对象比对 */
    @Schema(description = "文件 SHA-256 摘要（64 位十六进制）", example = "9f2d2b04a1c8e6f0a77d2c4b8e1f6a901234567890abcdef1234567890abcdef")
    @NotBlank(message = "SHA-256 摘要不能为空")
    @Pattern(regexp = "^[A-Fa-f0-9]{64}$", message = "SHA-256 需为 64 位十六进制字符串")
    private String sha256;

}
