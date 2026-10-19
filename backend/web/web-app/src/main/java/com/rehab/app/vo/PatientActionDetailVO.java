package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 患者端动作版本快照详情 VO（发布版本只读；下发算法参数、运动签名与评估规则）。
 */
@Schema(description = "患者端动作版本快照详情")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatientActionDetailVO extends PatientActionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 版本状态（仅 PUBLISHED 可见） */
    @Schema(description = "版本状态（仅 PUBLISHED 可见）")
    private String status;

    /** 算法配置（JSON 字符串，快照只读） */
    @Schema(description = "算法配置（JSON 字符串，快照只读）")
    private String algoConfig;

    /** 运动特征签名（JSON 字符串，快照只读） */
    @Schema(description = "运动特征签名（JSON 字符串，快照只读）")
    private String motionSignature;

    /** 姿态引擎标识 */
    @Schema(description = "姿态引擎标识")
    private String poseEngineId;

    /** 评估规则列表 */
    @Schema(description = "评估规则列表")
    private List<ActionRuleVO> rules;
}
