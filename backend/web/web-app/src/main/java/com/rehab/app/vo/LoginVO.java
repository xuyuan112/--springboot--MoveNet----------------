package com.rehab.app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录成功响应
 */
@Schema(description = "登录成功响应")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 访问令牌 */
    @Schema(description = "访问令牌")
    private String accessToken;

    /** 有效期（秒） */
    @Schema(description = "有效期（秒）")
    private Long expiresIn;

    /** 用户基本信息 */
    @Schema(description = "用户基本信息")
    private UserInfoVO user;
}
