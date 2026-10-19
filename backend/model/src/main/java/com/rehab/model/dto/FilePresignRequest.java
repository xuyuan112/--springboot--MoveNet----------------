package com.rehab.model.dto;

import com.rehab.model.enums.FilePurpose;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传预签名申请请求（POST /therapist/files/presign、/admin/files/presign，受类型/大小白名单限制）
 */
@Data
@Schema(description = "文件上传预签名申请请求（POST /therapist/files/presign、/admin/files/presign，受类型/大小白名单限制）")
public class FilePresignRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 原始文件名 */
    @Schema(description = "原始文件名", example = "膝关节训练指导.pdf")
    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名最长 255 个字符")
    private String fileName;

    /** MIME 类型（需在白名单内，如 application/pdf、video/mp4、image/png） */
    @Schema(description = "MIME 类型（需在白名单内）", example = "application/pdf")
    @NotBlank(message = "文件类型不能为空")
    @Size(max = 100, message = "文件类型最长 100 个字符")
    private String contentType;

    /** 文件大小（字节），需符合用途对应的大小上限 */
    @Schema(description = "文件大小（字节）", example = "524288")
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小需大于 0")
    private Long fileSize;

    /** 文件用途：TEMPLATE 模板 / ATTACHMENT 附件 / MESSAGE 消息 / AVATAR 头像 */
    @Schema(description = "文件用途", example = "MESSAGE")
    @NotNull(message = "文件用途不能为空")
    private FilePurpose purpose;

}
