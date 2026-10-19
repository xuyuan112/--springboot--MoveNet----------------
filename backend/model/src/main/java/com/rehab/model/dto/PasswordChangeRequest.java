package com.rehab.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改密码请求
 */
@Schema(description = "修改密码请求")
@Data
public class PasswordChangeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 原密码 */
    @Schema(description = "原密码", example = "Abc12345")
    @NotBlank(message = "原密码不能为空")
    @Size(max = 64, message = "原密码最长 64 个字符")
    private String oldPassword;

    /** 新密码（长度 8~64 位；修改成功后所有已签发令牌失效，需重新登录） */
    @Schema(description = "新密码（长度 8~64 位）", example = "NewPass123")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度需在 8~64 位之间")
    private String newPassword;
}
