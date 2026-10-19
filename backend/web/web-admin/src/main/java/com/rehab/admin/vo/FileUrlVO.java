package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件预签名预览地址响应（GET /admin/files/{fileId}/url）。
 */
@Data
@Schema(description = "文件预签名预览地址响应")
public class FileUrlVO {

    /** 文件ID */
    @Schema(description = "文件ID")
    private Long fileId;

    /** 原始文件名 */
    @Schema(description = "原始文件名")
    private String fileName;

    /** MIME 类型 */
    @Schema(description = "MIME 类型")
    private String contentType;

    /** 文件大小（字节） */
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /** 短时预签名下载地址 */
    @Schema(description = "短时预签名下载地址")
    private String url;

    /** URL 过期时间 */
    @Schema(description = "URL 过期时间")
    private LocalDateTime urlExpiresAt;
}
