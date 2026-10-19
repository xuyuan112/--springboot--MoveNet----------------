package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件预签名上传地址响应（POST /admin/files/presign）。
 */
@Data
@Schema(description = "文件预签名上传地址响应")
public class FilePresignVO {

    /** 直传会话ID（完成确认时回传） */
    @Schema(description = "直传会话ID（完成确认时回传）")
    private String uploadId;

    /** MinIO 预签名 PUT 地址（前端直传，开发环境走 /minio 同源代理） */
    @Schema(description = "预签名 PUT 上传地址")
    private String uploadUrl;

    /** HTTP 方法，固定 PUT */
    @Schema(description = "HTTP 方法", example = "PUT")
    private String method;

    /** 直传时必须携带的请求头（Content-Type 需与申报一致） */
    @Schema(description = "直传时必须携带的请求头")
    private java.util.Map<String, String> headers;

    /** 预签名有效期（秒） */
    @Schema(description = "预签名有效期（秒）", example = "600")
    private Integer expiresInSec;
}
