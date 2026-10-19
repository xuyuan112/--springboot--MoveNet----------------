package com.rehab.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置项（前缀 {@code rehab.jwt}）。
 *
 * <p>生产环境必须通过环境变量 {@code JWT_SECRET} 覆盖开发默认密钥，
 * 密钥长度需满足 HS256 要求（&ge; 256 bit / 32 字节）。</p>
 */
@Data
@ConfigurationProperties(prefix = "rehab.jwt")
public class JwtProperties {

    /** 签名密钥（原始字符串，至少 32 字节；生产用 JWT_SECRET 注入） */
    private String secret = "rehab-platform-default-jwt-secret-key-change-me-in-prod-0123456789";

    /** access token 有效期（秒），默认 1800 秒=30 分钟 */
    private Long accessTokenExpireSec = 1800L;

    /** 请求头名称 */
    private String header = "Authorization";

    /** token 前缀 */
    private String tokenPrefix = "Bearer ";
}
