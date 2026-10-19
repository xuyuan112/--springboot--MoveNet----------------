package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端账号列表项
 */
@Data
@Builder
@Schema(description = "管理端账号列表项")
public class AdminUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户账号ID */
    @Schema(description = "用户账号ID")
    private Long userId;

    /** 登录名 */
    @Schema(description = "登录名")
    private String username;

    /** 角色码：PATIENT/THERAPIST/ADMIN */
    @Schema(description = "角色码：PATIENT/THERAPIST/ADMIN")
    private String roleCode;

    /** 角色中文名 */
    @Schema(description = "角色中文名")
    private String roleName;

    /** 手机号（脱敏：138****8000） */
    @Schema(description = "手机号（脱敏）")
    private String phone;

    /** 真实姓名（取角色档案，缺省回退登录名） */
    @Schema(description = "真实姓名")
    private String realName;

    /** 状态：1 启用 / 0 禁用 */
    @Schema(description = "状态：1 启用 / 0 禁用")
    private Integer status;

    /** 最后登录时间 */
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
