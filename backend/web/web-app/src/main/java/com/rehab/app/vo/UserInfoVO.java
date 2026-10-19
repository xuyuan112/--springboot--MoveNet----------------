package com.rehab.app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前登录用户信息
 */
@Schema(description = "当前登录用户信息")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @Schema(description = "用户 ID")
    private Long id;

    /** 角色：PATIENT / THERAPIST / ADMIN */
    @Schema(description = "角色：PATIENT / THERAPIST / ADMIN")
    private String role;

    /** 展示名称（患者/治疗师取档案真实姓名，管理员取登录名） */
    @Schema(description = "展示名称（患者/治疗师取档案真实姓名，管理员取登录名）")
    private String name;
}
