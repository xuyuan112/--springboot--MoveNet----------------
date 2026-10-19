package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端 - 动作详情 VO（含版本列表）
 */
@Data
@Builder
@Schema(description = "管理端-动作详情")
public class AdminActionDetailVO implements Serializable {

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

    @Schema(description = "动作说明")
    private String description;

    @Schema(description = "创建人用户ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "版本列表（按 versionNo 降序）")
    private List<AdminActionVersionVO> versionList;
}
