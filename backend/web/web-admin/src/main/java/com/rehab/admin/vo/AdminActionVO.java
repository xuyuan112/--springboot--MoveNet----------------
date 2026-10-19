package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端 - 动作列表 VO
 */
@Data
@Builder
@Schema(description = "管理端-动作列表项")
public class AdminActionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "动作ID")
    private Long actionId;

    @Schema(description = "动作名称")
    private String name;

    @Schema(description = "目标康复部位编码")
    private String targetPartCode;

    @Schema(description = "目标康复部位名称")
    private String targetPartName;

    @Schema(description = "难度：EASY/MEDIUM/HARD")
    private String difficulty;

    @Schema(description = "难度名称")
    private String difficultyName;

    @Schema(description = "状态：DRAFT/PUBLISHED/DISABLED")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
