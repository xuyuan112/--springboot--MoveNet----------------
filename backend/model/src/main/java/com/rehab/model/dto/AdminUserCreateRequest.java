package com.rehab.model.dto;

import com.rehab.model.enums.Gender;
import com.rehab.model.enums.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端创建账号请求（POST /admin/users，仅限创建患者/治疗师账号）
 */
@Data
@Schema(description = "管理端创建账号请求（POST /admin/users，仅限创建患者/治疗师账号）")
public class AdminUserCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 登录名（4~50 位字母/数字/下划线） */
    @Schema(description = "登录名（4~50 位字母/数字/下划线）", example = "patient01")
    @NotBlank(message = "登录名不能为空")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{3,49}$", message = "登录名需以字母开头，仅含字母/数字/下划线，长度 4~50 位")
    private String username;

    /** 角色：仅可创建 PATIENT 或 THERAPIST（不可通过此接口创建 ADMIN） */
    @Schema(description = "角色：PATIENT 或 THERAPIST", example = "PATIENT")
    @NotNull(message = "角色不能为空")
    private RoleCode roleCode;

    /** 手机号（可选） */
    @Schema(description = "手机号（可选）", example = "13800138000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 真实姓名 */
    @Schema(description = "真实姓名", example = "张三")
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名最长 50 个字符")
    private String realName;

    /** 性别（创建患者时使用） */
    @Schema(description = "性别（创建患者时使用）")
    private Gender gender;

    /** 出生日期（患者） */
    @Schema(description = "出生日期（患者）")
    private LocalDate birthDate;

    /** 身高(cm)（患者） */
    @Schema(description = "身高(cm)（患者）")
    private BigDecimal heightCm;

    /** 体重(kg)（患者） */
    @Schema(description = "体重(kg)（患者）")
    private BigDecimal weightKg;

    /** 既往病史（患者） */
    @Schema(description = "既往病史（患者）")
    @Size(max = 1000)
    private String medicalHistory;

    /** 过敏史（患者） */
    @Schema(description = "过敏史（患者）")
    @Size(max = 500)
    private String allergyInfo;

    /** 禁忌症（患者） */
    @Schema(description = "禁忌症（患者）")
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
