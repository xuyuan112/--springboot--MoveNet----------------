package com.rehab.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 康复计划创建请求（POST /therapist/patients/{patientId}/plans，初始为 DRAFT）
 */
@Data
@Schema(description = "康复计划创建请求（POST /therapist/patients/{patientId}/plans，初始为 DRAFT）")
public class PlanCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 计划名称 */
    @Schema(description = "计划名称", example = "膝关节恢复第1阶段")
    @NotBlank(message = "计划名称不能为空")
    @Size(max = 100, message = "计划名称最长 100 个字符")
    private String name;

    /** 康复部位编码 */
    @Schema(description = "康复部位编码", example = "KNEE")
    @Size(max = 30, message = "康复部位编码最长 30 个字符")
    private String rehabPartCode;

    /** 开始日期（yyyy-MM-dd） */
    @Schema(description = "开始日期（yyyy-MM-dd）", example = "2026-09-15")
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /** 结束日期（yyyy-MM-dd，不早于开始日期） */
    @Schema(description = "结束日期（yyyy-MM-dd，不早于开始日期）", example = "2026-10-15")
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /** 注意事项 / 备注 */
    @Schema(description = "注意事项 / 备注", example = "出现疼痛立即停止")
    @Size(max = 500, message = "注意事项最长 500 个字符")
    private String remark;

    /** 计划动作项（至少一项） */
    @Schema(description = "计划动作项（至少一项）")
    @NotEmpty(message = "计划至少包含一个动作项")
    @Valid
    private List<PlanItemPayload> items;
    /**
     * 计划动作项载荷（创建计划时内嵌提交）
     */
    @Data
    @Schema(description = "计划动作项载荷")
    public static class PlanItemPayload implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 动作版本ID（必须为已发布版本） */
        @Schema(description = "动作版本ID（必须为已发布版本）", example = "7")
        @NotNull(message = "动作版本ID不能为空")
        private Long actionVersionId;

        /** 组数 */
        @Schema(description = "组数", example = "3")
        @NotNull(message = "组数不能为空")
        @Min(value = 1, message = "组数至少为 1")
        private Integer sets;

        /** 每组目标次数 */
        @Schema(description = "每组目标次数", example = "10")
        @NotNull(message = "目标次数不能为空")
        @Min(value = 1, message = "目标次数至少为 1")
        private Integer targetReps;

        /** 目标幅度（度） */
        @Schema(description = "目标幅度（度）", example = "90")
        private BigDecimal targetAngle;

        /** 组间休息（秒） */
        @Schema(description = "组间休息（秒）", example = "30")
        @Min(value = 0, message = "休息时长不能为负")
        private Integer restSec;
    }
}
