package com.rehab.model.dto;

import com.rehab.model.enums.FrequencyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 计划执行日程设置请求（PUT /therapist/plans/{planId}/schedule）
 */
@Data
@Schema(description = "计划执行日程设置请求（PUT /therapist/plans/{planId}/schedule）")
public class PlanScheduleRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 频次类型：DAILY 每日 / WEEKLY 每周 / CUSTOM 自定义 */
    @Schema(description = "频次类型：DAILY 每日 / WEEKLY 每周 / CUSTOM 自定义", example = "WEEKLY")
    @NotNull(message = "频次类型不能为空")
    private FrequencyType frequencyType;

    /** 频次值（如每周训练次数） */
    @Schema(description = "频次值（如每周训练次数）", example = "3")
    @Min(value = 1, message = "频次值至少为 1")
    private Integer frequencyValue;

    /** 执行星期（1 周一 ~ 7 周日），WEEKLY/CUSTOM 时必填 */
    @Schema(description = "执行星期（1 周一 ~ 7 周日）", example = "[1, 3, 5]")
    private List<@Min(value = 1, message = "星期取值为 1~7") @Max(value = 7, message = "星期取值为 1~7") Integer> weekdays;

}
