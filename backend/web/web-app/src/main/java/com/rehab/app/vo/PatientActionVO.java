package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 患者端动作库列表项 VO（仅 PUBLISHED 动作及其 PUBLISHED 版本）。
 */
@Schema(description = "患者端动作库列表项")
@Data
public class PatientActionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 动作ID */
    @Schema(description = "动作ID")
    private Long actionId;

    /** 动作版本ID */
    @Schema(description = "动作版本ID")
    private Long actionVersionId;

    /** 动作名称 */
    @Schema(description = "动作名称")
    private String name;

    /** 动作说明 */
    @Schema(description = "动作说明")
    private String description;

    /** 目标康复部位编码 */
    @Schema(description = "目标康复部位编码")
    private String targetPartCode;

    /** 康复部位名称 */
    @Schema(description = "康复部位名称")
    private String partName;

    /** 难度（EASY/MEDIUM/HARD） */
    @Schema(description = "难度（EASY/MEDIUM/HARD）")
    private String difficulty;

    /** 难度名称 */
    @Schema(description = "难度名称")
    private String difficultyName;

    /** 动作类型（ROM/STRENGTH/BALANCE） */
    @Schema(description = "动作类型（ROM/STRENGTH/BALANCE）")
    private String actionType;

    /** 版本号 */
    @Schema(description = "版本号")
    private Integer versionNo;

    /** 版本更新时间 */
    @Schema(description = "版本更新时间")
    private LocalDateTime updatedAt;
}
