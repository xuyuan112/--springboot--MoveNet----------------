package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 治疗师端患者详情 VO（GET /therapist/patients/{patientId}）。
 *
 * <p>包含档案、绑定信息与风险提示；手机号脱敏。病史/过敏/禁忌对治疗师可见
 * （制定计划必要信息），但风险提示文案规避医学诊断表述。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "治疗师端患者详情")
public class TherapistPatientDetailVO extends TherapistPatientVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "身高(cm)")
    private java.math.BigDecimal heightCm;

    @Schema(description = "体重(kg)")
    private java.math.BigDecimal weightKg;

    @Schema(description = "既往病史")
    private String medicalHistory;

    @Schema(description = "过敏史")
    private String allergyInfo;

    @Schema(description = "禁忌症")
    private String contraindication;

    @Schema(description = "风险提示列表（量化口径启发式提示，非医学诊断）")
    private List<String> riskHints;
}
