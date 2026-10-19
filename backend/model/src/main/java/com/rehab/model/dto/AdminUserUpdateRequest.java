package com.rehab.model.dto;

import com.rehab.model.enums.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端修改账号非敏感资料请求（PUT /admin/users/{userId}，字段可选；用户名/角色/密码不在此修改）
 */
@Data
@Schema(description = "管理端修改账号非敏感资料请求（PUT /admin/users/{userId}，字段可选；用户名/角色/密码不在此修改）")
public class AdminUserUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 手机号 */
    @Schema(description = "手机号")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 真实姓名 */
    @Schema(description = "真实姓名")
    @Size(max = 50)
    private String realName;

    /** 性别 */
    @Schema(description = "性别")
    private Gender gender;

    /** 出生日期 */
    @Schema(description = "出生日期")
    private LocalDate birthDate;

    /** 身高(cm) */
    @Schema(description = "身高(cm)")
    private BigDecimal heightCm;

    /** 体重(kg) */
    @Schema(description = "体重(kg)")
    private BigDecimal weightKg;

    /** 既往病史 */
    @Schema(description = "既往病史")
    @Size(max = 1000)
    private String medicalHistory;

    /** 过敏史 */
    @Schema(description = "过敏史")
    @Size(max = 500)
    private String allergyInfo;

    /** 禁忌症 */
    @Schema(description = "禁忌症")
    @Size(max = 500)
    private String contraindication;

    /** 职称（治疗师） */
    @Schema(description = "职称（治疗师）")
    @Size(max = 50)
    private String title;

    /** 专长领域（治疗师） */
    @Schema(description = "专长领域（治疗师）")
    @Size(max = 200)
    private String specialty;

    /** 执业证书编号（治疗师） */
    @Schema(description = "执业证书编号（治疗师）")
    @Size(max = 50)
    private String licenseNo;

    /** 个人简介（治疗师） */
    @Schema(description = "个人简介（治疗师）")
    @Size(max = 500)
    private String introduction;

}
