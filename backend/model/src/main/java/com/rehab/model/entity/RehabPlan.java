package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 康复计划
 */
@Schema(description = "康复计划")
@Data
@TableName("rehab_plan")
public class RehabPlan {

    /** 计划ID */
    @Schema(description = "计划ID")
    @TableId(value = "plan_id", type = IdType.AUTO)
    private Long planId;

    /** 患者用户ID */
    @Schema(description = "患者用户ID")
    private Long patientId;

    /** 治疗师用户ID */
    @Schema(description = "治疗师用户ID")
    private Long therapistId;

    /** 患者治疗师绑定ID */
    @Schema(description = "患者治疗师绑定ID")
    private Long bindingId;

    /** 计划名称 */
    @Schema(description = "计划名称")
    private String name;

    /** 康复部位编码 */
    @Schema(description = "康复部位编码")
    private String rehabPartCode;

    /** 计划版本号（调整计划时递增） */
    @Schema(description = "计划版本号（调整计划时递增）")
    private Integer versionNo;

    /** 开始日期 */
    @Schema(description = "开始日期")
    private LocalDate startDate;

    /** 结束日期 */
    @Schema(description = "结束日期")
    private LocalDate endDate;

    /** 频次类型：DAILY每日/WEEKLY每周/CUSTOM自定义 */
    @Schema(description = "频次类型：DAILY每日/WEEKLY每周/CUSTOM自定义")
    private String frequencyType;

    /** 频次值（如每周N次） */
    @Schema(description = "频次值（如每周N次）")
    private Integer frequencyValue;

    /** 状态：ACTIVE/COMPLETED/SUSPENDED/CANCELLED */
    @Schema(description = "状态：ACTIVE/COMPLETED/SUSPENDED/CANCELLED")
    private String status;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

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
