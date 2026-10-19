package com.rehab.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告发布请求（POST /admin/announcements/{id}/publish）：发布并设置有效期。
 */
@Data
@Schema(description = "公告发布请求")
public class AnnouncementPublishRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 展示开始时间 */
    @Schema(description = "展示开始时间，格式 yyyy-MM-dd HH:mm:ss", example = "2026-09-26 00:00:00")
    @NotNull(message = "展示开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 展示结束时间 */
    @Schema(description = "展示结束时间，格式 yyyy-MM-dd HH:mm:ss", example = "2026-10-26 23:59:59")
    @NotNull(message = "展示结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
