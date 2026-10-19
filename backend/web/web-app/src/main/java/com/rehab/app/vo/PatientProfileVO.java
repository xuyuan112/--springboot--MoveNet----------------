package com.rehab.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者本人资料视图对象（手机号脱敏）
 */
@Schema(description = "患者本人资料视图对象")
@Data
public class PatientProfileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户账号ID */
    @Schema(description = "用户账号ID")
    private Long userId;

    /** 登录名 */
    @Schema(description = "登录名")
    private String username;

    /** 手机号（脱敏：138****8000） */
    @Schema(description = "手机号（脱敏）")
    private String phone;

    /** 真实姓名 */
    @Schema(description = "真实姓名")
    private String name;

    /** 性别：M男/F女/U未知 */
    @Schema(description = "性别：M男/F女/U未知")
    private String gender;

    /** 出生日期 */
    @Schema(description = "出生日期")
    private LocalDate birthday;

    /** 身高(cm) */
    @Schema(description = "身高(cm)")
    private BigDecimal heightCm;

    /** 体重(kg) */
    @Schema(description = "体重(kg)")
    private BigDecimal weightKg;

    /** 主康复部位编码 */
    @Schema(description = "主康复部位编码")
    private String rehabPartCode;

    /** 主康复部位名称 */
    @Schema(description = "主康复部位名称")
    private String rehabPartName;

    /** 既往病史摘要 */
    @Schema(description = "既往病史摘要")
    private String medicalSummary;

    /** 过敏史 */
    @Schema(description = "过敏史")
    private String allergyInfo;

    /** 禁忌症 */
    @Schema(description = "禁忌症")
    private String contraindications;

    /** 最后登录时间 */
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    /** 资料更新时间 */
    @Schema(description = "资料更新时间")
    private LocalDateTime updatedAt;
}
