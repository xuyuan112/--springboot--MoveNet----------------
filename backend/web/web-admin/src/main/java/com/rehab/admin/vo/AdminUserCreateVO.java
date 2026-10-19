package com.rehab.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 创建账号结果（不回显初始密码明文，仅返回下发状态）
 */
@Data
@Builder
@Schema(description = "创建账号结果（不回显初始密码明文，仅返回下发状态）")
public class AdminUserCreateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 新建账号ID */
    @Schema(description = "新建账号ID")
    private Long userId;

    /** 登录名 */
    @Schema(description = "登录名")
    private String username;

    /** 角色码 */
    @Schema(description = "角色码")
    private String roleCode;

    /**
     * 初始密码是否已线下交付给用户。
     * 创建时默认 false（系统初始密码通过线下渠道告知），管理员交付后可在业务上标记。
     */
    @Schema(description = "初始密码是否已线下交付给用户")
    private Boolean initialPasswordDelivered;

    /** 首次登录是否需要修改密码（平台策略固定 true） */
    @Schema(description = "首次登录是否需要修改密码")
    private Boolean mustChangePassword;
}
