package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 治疗师端患者训练异常提示 VO（GET /therapist/patients/{patientId}/alerts）。
 *
 * <p>首版量化口径（需求 3.2.1）：单次 total_score&lt;60、连续 3 天无 COMPLETED 任务、
 * 备注/咨询含疼痛反馈。所有文案仅作训练管理提示，不构成医学诊断。</p>
 */
@Data
@Schema(description = "治疗师端患者训练异常提示")
public class TherapistAlertVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "提示类型：LOW_SCORE低分/NO_COMPLETION_3D连续3天未完成/PAIN_FEEDBACK疼痛反馈")
    private String alertType;

    @Schema(description = "提示类型名称")
    private String alertTypeName;

    @Schema(description = "发生日期")
    private LocalDate alertDate;

    @Schema(description = "提示内容（规避医学诊断表述）")
    private String content;

    @Schema(description = "关联对象ID（记录ID/任务ID等，可为空）")
    private Long refId;
}
