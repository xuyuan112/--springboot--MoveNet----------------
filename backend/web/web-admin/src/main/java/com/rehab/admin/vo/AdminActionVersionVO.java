package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端 - 动作版本列表 VO
 */
@Data
@Builder
@Schema(description = "管理端-动作版本列表项")
public class AdminActionVersionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "动作版本ID")
    private Long versionId;

    @Schema(description = "动作ID")
    private Long actionId;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "动作类型：ROM/STRENGTH/BALANCE")
    private String actionType;

    @Schema(description = "动作类型名称")
    private String actionTypeName;

    @Schema(description = "姿态引擎标识")
    private String poseEngineId;

    @Schema(description = "状态：DRAFT/PUBLISHED/DEPRECATED")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
