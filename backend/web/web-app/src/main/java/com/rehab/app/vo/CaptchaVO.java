package com.rehab.app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 图形验证码响应：
 * 前端展示 {@link #image}，登录时将 {@link #captchaId} 与用户输入一并回传。
 */
@Schema(description = "图形验证码响应")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 验证码唯一标识（Redis 键的一部分），登录时原样回传 */
    @Schema(description = "验证码唯一标识（Redis 键的一部分），登录时原样回传")
    private String captchaId;

    /** 验证码图片 Data URI（data:image/png;base64,...），可直接赋给 img.src */
    @Schema(description = "验证码图片 Data URI（data:image/png;base64,...），可直接赋给 img.src")
    private String image;

    /** 有效期（秒），过期需重新获取 */
    @Schema(description = "有效期（秒），过期需重新获取")
    private Long expireSeconds;
}
