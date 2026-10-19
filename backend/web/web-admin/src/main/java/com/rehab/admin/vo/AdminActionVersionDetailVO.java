package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端 - 动作版本详情 VO（含规则与模板列表）
 */
@Data
@Builder
@Schema(description = "管理端-动作版本详情")
public class AdminActionVersionDetailVO implements Serializable {

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

    @Schema(description = "算法配置 JSON")
    private String algoConfig;

    @Schema(description = "运动特征签名 JSON")
    private String motionSignature;

    @Schema(description = "创建人用户ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "规则列表")
    private List<AdminActionRuleVO> ruleList;

    @Schema(description = "模板列表")
    private List<AdminActionTemplateVO> templateList;
}
