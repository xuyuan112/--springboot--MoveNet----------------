package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 患者/治疗师端公告 VO（仅当前有效公告）。
 */
@Data
@Schema(description = "端侧公告")
public class AppAnnouncementVO {

    @Schema(description = "公告ID")
    private Long announcementId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "公告正文")
    private String content;

    @Schema(description = "展示开始时间")
    private LocalDateTime startTime;

    @Schema(description = "展示结束时间")
    private LocalDateTime endTime;

    @Schema(description = "发布时间（记录创建时间）")
    private LocalDateTime createdAt;
}
