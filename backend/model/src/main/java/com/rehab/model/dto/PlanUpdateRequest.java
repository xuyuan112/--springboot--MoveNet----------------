package com.rehab.model.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 草稿计划基本信息修改请求（PUT /therapist/plans/{planId}，仅 DRAFT 可改，字段可选）
 */
@Data
@Schema(description = "草稿计划基本信息修改请求（PUT /therapist/plans/{planId}，仅 DRAFT 可改，字段可选）")
public class PlanUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 计划名称 */
    @Schema(description = "计划名称")
    @Size(max = 100, message = "计划名称最长 100 个字符")
    private String name;

    /** 康复部位编码 */
    @Schema(description = "康复部位编码")
    @Size(max = 30)
    private String rehabPartCode;

    /** 开始日期 */
    @Schema(description = "开始日期")
    private LocalDate startDate;

    /** 结束日期 */
    @Schema(description = "结束日期")
    private LocalDate endDate;

    /** 注意事项 / 备注 */
    @Schema(description = "注意事项 / 备注")
    @Size(max = 500)
    private String remark;

}
