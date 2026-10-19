package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端账号详情（账号 + 角色档案）
 */
@Data
@Builder
@Schema(description = "管理端账号详情（账号 + 角色档案）")
public class AdminUserDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户账号ID */
    @Schema(description = "用户账号ID")
    private Long userId;

    /** 登录名 */
    @Schema(description = "登录名")
    private String username;

    /** 角色码 */
    @Schema(description = "角色码")
    private String roleCode;

    /** 角色中文名 */
    @Schema(description = "角色中文名")
    private String roleName;

    /** 手机号（管理员可见完整号码） */
    @Schema(description = "手机号")
    private String phone;

    /** 状态：1 启用 / 0 禁用 */
    @Schema(description = "状态：1 启用 / 0 禁用")
    private Integer status;

    /** 真实姓名 */
    @Schema(description = "真实姓名")
    private String realName;

    /** 性别：M/F/U（患者） */
    @Schema(description = "性别：M/F/U（患者）")
    private String gender;

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
    private String medicalHistory;

    /** 过敏史（患者） */
    @Schema(description = "过敏史（患者）")
    private String allergyInfo;

    /** 禁忌症（患者） */
    @Schema(description = "禁忌症（患者）")
    private String contraindication;

    /** 职称（治疗师） */
    @Schema(description = "职称（治疗师）")
    private String title;

    /** 专长领域（治疗师） */
    @Schema(description = "专长领域（治疗师）")
    private String specialty;

    /** 执业证书编号（治疗师） */
    @Schema(description = "执业证书编号（治疗师）")
    private String licenseNo;

    /** 个人简介（治疗师） */
    @Schema(description = "个人简介（治疗师）")
    private String introduction;

    /** 最后登录时间 */
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
