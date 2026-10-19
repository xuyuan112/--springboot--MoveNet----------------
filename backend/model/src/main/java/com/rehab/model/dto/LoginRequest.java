package com.rehab.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录请求
 */
@Schema(description = "登录请求")
@Data
public class LoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 登录名 */
    @Schema(description = "登录名", example = "admin")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名最长 50 个字符")
    private String username;

    /** 明文密码（传输层由 HTTPS 保护，服务端 BCrypt 校验） */
    @Schema(description = "明文密码（传输层由 HTTPS 保护，服务端 BCrypt 校验）", example = "Abc12345")
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码最长 64 个字符")
    private String password;

    /** 图形验证码标识（GET /auth/captcha 返回的 captchaId） */
    @Schema(description = "图形验证码标识（GET /auth/captcha 返回的 captchaId）",
            example = "550e8400b3424b9d8c2e9f0a1b2c3d4e")
    @NotBlank(message = "验证码标识不能为空")
    @Size(max = 64, message = "验证码标识非法")
    private String captchaId;

    /** 用户输入的图形验证码（不区分大小写，一次性消费） */
    @Schema(description = "用户输入的图形验证码（不区分大小写，一次性消费）", example = "a1b2")
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 6, message = "验证码长度应为 4 位")
    private String captchaCode;
}
