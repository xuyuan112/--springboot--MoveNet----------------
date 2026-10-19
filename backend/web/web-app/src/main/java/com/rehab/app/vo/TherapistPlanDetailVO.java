package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 治疗师端计划详情 VO（GET /therapist/plans/{planId}）：
 * 计划基本信息 + 动作项全量 + 执行星期。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "治疗师端计划详情")
public class TherapistPlanDetailVO extends TherapistPlanVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "执行星期（1周一~7周日，DAILY 为空）")
    private List<Integer> weekdays;

    @Schema(description = "动作项列表（按顺序号升序）")
    private List<TherapistPlanItemVO> items;
}
