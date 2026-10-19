package com.rehab.model.dto;

import com.rehab.model.enums.Gender;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 患者本人资料更新请求（PUT /patient/profile，仅允许修改本人开放字段）
 */
@Data
@Schema(description = "患者本人资料更新请求（PUT /patient/profile，仅允许修改本人开放字段）")
public class PatientProfileUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 真实姓名 */
    @Schema(description = "真实姓名", example = "张三")
    @Size(max = 50, message = "姓名最长 50 个字符")
    private String name;

    /** 性别：M 男 / F 女 / U 未知 */
    @Schema(description = "性别：M 男 / F 女 / U 未知", example = "M")
    private Gender gender;

    /** 出生日期（ISO-8601 日期 yyyy-MM-dd） */
    @Schema(description = "出生日期（ISO-8601 日期 yyyy-MM-dd）", example = "1990-01-01")
    private LocalDate birthday;

    /** 身高(cm)，范围 50~250 */
    @Schema(description = "身高(cm)，范围 50~250", example = "170")
    @DecimalMin(value = "50", message = "身高需在 50~250cm 之间")
    @DecimalMax(value = "250", message = "身高需在 50~250cm 之间")
    private BigDecimal heightCm;

    /** 体重(kg)，范围 10~300 */
    @Schema(description = "体重(kg)，范围 10~300", example = "65")
    @DecimalMin(value = "10", message = "体重需在 10~300kg 之间")
    @DecimalMax(value = "300", message = "体重需在 10~300kg 之间")
    private BigDecimal weightKg;

    /** 主康复部位编码（需为启用的字典编码，如 KNEE） */
    @Schema(description = "主康复部位编码（需为启用的字典编码，如 KNEE）", example = "KNEE")
    @Size(max = 30, message = "康复部位编码最长 30 个字符")
    private String rehabPartCode;

    /** 既往病史摘要（最小必要原则） */
    @Schema(description = "既往病史摘要（最小必要原则）")
    @Size(max = 1000, message = "病史最长 1000 个字符")
    private String medicalSummary;

    /** 过敏史 */
    @Schema(description = "过敏史")
    @Size(max = 500, message = "过敏史最长 500 个字符")
    private String allergyInfo;

    /** 禁忌症 */
    @Schema(description = "禁忌症")
    @Size(max = 500, message = "禁忌症最长 500 个字符")
    private String contraindications;

}
