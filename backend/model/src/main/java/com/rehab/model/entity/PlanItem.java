package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 计划动作项
 */
@Schema(description = "计划动作项")
@Data
@TableName("plan_item")
public class PlanItem {

    /** 计划项ID */
    @Schema(description = "计划项ID")
    @TableId(value = "plan_item_id", type = IdType.AUTO)
    private Long planItemId;

    /** 计划ID */
    @Schema(description = "计划ID")
    private Long planId;

    /** 动作版本ID */
    @Schema(description = "动作版本ID")
    private Long actionVersionId;

    /** 动作顺序号 */
    @Schema(description = "动作顺序号")
    private Integer sequenceNo;

    /** 目标组数 */
    @Schema(description = "目标组数")
    private Integer targetSets;

    /** 目标次数/组 */
    @Schema(description = "目标次数/组")
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

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /** 逻辑删除：0正常1已删 */
    @Schema(description = "逻辑删除：0正常1已删")
    @TableLogic
    private Integer deleted;
}
