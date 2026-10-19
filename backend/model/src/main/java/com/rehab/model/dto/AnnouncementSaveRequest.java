package com.rehab.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告创建/修改请求（POST、PUT /admin/announcements）。
 *
 * <p>草稿阶段有效期可为空（服务端兜底 now ~ now+30 天）；
 * 发布时以 {@link AnnouncementPublishRequest} 显式指定有效期。</p>
 */
@Data
@Schema(description = "公告创建/修改请求")
public class AnnouncementSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标题 */
    @Schema(description = "标题", maxLength = 200)
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题最长 200 个字符")
    private String title;

    /** 公告正文 */
    @Schema(description = "公告正文")
    @NotBlank(message = "公告正文不能为空")
    private String content;

    /** 可见范围：ALL / PATIENT / THERAPIST */
    @Schema(description = "可见范围：ALL 全部 / PATIENT 患者 / THERAPIST 治疗师", example = "ALL")
    @NotBlank(message = "可见范围不能为空")
    @Pattern(regexp = "ALL|PATIENT|THERAPIST", message = "scope 仅支持 ALL、PATIENT、THERAPIST")
    private String scope;

    /** 展示开始时间（草稿可空，发布时必填） */
    @Schema(description = "展示开始时间，格式 yyyy-MM-dd HH:mm:ss（草稿可空）", example = "2026-09-26 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 展示结束时间（草稿可空，发布时必填） */
    @Schema(description = "展示结束时间，格式 yyyy-MM-dd HH:mm:ss（草稿可空）", example = "2026-10-26 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
