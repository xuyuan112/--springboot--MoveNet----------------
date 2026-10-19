package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 开始训练时返回的动作版本参数快照 VO。
 *
 * <p>按 {@code plan_daily_task → plan_item → action_version(algo_config) → action_rule → action_template}
 * 固定顺序读取并一次性下发，不读最新动作配置，保证可复现（需求 6.3.1）。</p>
 */
@Schema(description = "任务动作参数快照")
@Data
public class TaskActionSnapshotVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    @Schema(description = "任务ID")
    private Long taskId;

    /** 计划项ID */
    @Schema(description = "计划项ID")
    private Long planItemId;

    /** 动作版本ID */
    @Schema(description = "动作版本ID")
    private Long actionVersionId;

    /** 动作名称 */
    @Schema(description = "动作名称")
    private String actionName;

    /** 目标组数（快照） */
    @Schema(description = "目标组数（快照）")
    private Integer targetSets;

    /** 目标次数（快照） */
    @Schema(description = "目标次数（快照）")
    private Integer targetReps;

    /** 目标幅度(度) */
    @Schema(description = "目标幅度(度)")
    private BigDecimal targetAmplitude;

    /** 单组时长(秒) */
    @Schema(description = "单组时长(秒)")
    private Integer durationSec;

    /** 组间休息(秒) */
    @Schema(description = "组间休息(秒)")
    private Integer restSec;

    /** 动作指导语 */
    @Schema(description = "动作指导语")
    private String instruction;

    /** 算法配置 JSON 字符串 */
    @Schema(description = "算法配置 JSON 字符串")
    private String algoConfig;

    /** 运动特征签名 JSON 字符串 */
    @Schema(description = "运动特征签名 JSON 字符串")
    private String motionSignature;

    /** 姿态引擎标识 */
    @Schema(description = "姿态引擎标识")
    private String poseEngineId;

    /** 动作规则列表 */
    @Schema(description = "动作规则列表")
    private List<ActionRuleVO> rules;

    /** 标准动作模板（可空） */
    @Schema(description = "标准动作模板（可空）")
    private ActionTemplateVO template;
}
