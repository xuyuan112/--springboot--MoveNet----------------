package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * MinIO文件对象
 */
@Schema(description = "MinIO文件对象")
@Data
@TableName("file_object")
public class FileObject {

    /** 文件ID */
    @Schema(description = "文件ID")
    @TableId(value = "file_id", type = IdType.AUTO)
    private Long fileId;

    /** MinIO存储桶 */
    @Schema(description = "MinIO存储桶")
    private String bucketName;

    /** 对象键 */
    @Schema(description = "对象键")
    private String objectKey;

    /** 原始文件名 */
    @Schema(description = "原始文件名")
    private String fileName;

    /** MIME类型 */
    @Schema(description = "MIME类型")
    private String contentType;

    /** 文件大小(字节) */
    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    /** SHA-256摘要 */
    @Schema(description = "SHA-256摘要")
    private String sha256;

    /** 用途：TEMPLATE样本/ATTACHMENT附件/MESSAGE消息/AVATAR头像 */
    @Schema(description = "用途：TEMPLATE样本/ATTACHMENT附件/MESSAGE消息/AVATAR头像")
    private String purpose;

    /** 安全扫描状态：PENDING/PASSED/FAILED */
    @Schema(description = "安全扫描状态：PENDING/PASSED/FAILED")
    private String scanStatus;

    /** 上传人用户ID */
    @Schema(description = "上传人用户ID")
    private Long createdBy;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 删除时间（NULL未删） */
    @Schema(description = "删除时间（NULL未删）")
    private LocalDateTime deletedAt;
}
