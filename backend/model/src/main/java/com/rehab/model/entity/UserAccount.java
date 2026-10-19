package com.rehab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户账号
 */
@Schema(description = "用户账号")
@Data
@TableName("user_account")
public class UserAccount {

    /** 用户账号ID */
    @Schema(description = "用户账号ID")
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /** 登录名 */
    @Schema(description = "登录名")
    private String username;

    /** BCrypt密码密文 */
    @Schema(description = "BCrypt密码密文")
    private String passwordHash;

    /** 角色：PATIENT/THERAPIST/ADMIN */
    @Schema(description = "角色：PATIENT/THERAPIST/ADMIN")
    private String roleCode;

    /** 手机号 */
    @Schema(description = "手机号")
    private String phone;

    /** 1启用0禁用 */
    @Schema(description = "1启用0禁用")
    private Integer status;

    /** 最后登录时间 */
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /** 逻辑删除：0正常1已删 */
    @Schema(description = "逻辑删除：0正常1已删")
    @TableLogic
    private Integer deleted;
}
