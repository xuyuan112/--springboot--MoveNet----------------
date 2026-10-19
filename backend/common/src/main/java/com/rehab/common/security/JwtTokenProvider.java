package com.rehab.common.security;

import com.rehab.common.config.JwtProperties;
import com.rehab.common.result.ResultCode;
import com.rehab.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析（jjwt 0.12.x API）。
 *
 * <p>令牌 claims：sub=username、userId、role、jti（唯一标识，用于 Redis 会话校验与登出失效）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("rehab.jwt.secret 长度不能小于 32 字节（HS256 要求）");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 access token
     *
     * @return token 字符串
     */
    public String createAccessToken(LoginUser loginUser) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getAccessTokenExpireSec() * 1000L);
        return Jwts.builder()
                .subject(loginUser.getUsername())
                .claim("userId", loginUser.getUserId())
                .claim("role", loginUser.getRoleCode())
                .id(UUID.randomUUID().toString().replace("-", ""))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * 解析并校验令牌签名/过期时间，失败统一抛业务异常（由过滤器转为 401）。
     * 过期返回 {@code TOKEN_EXPIRED}，其余非法情形返回 {@code TOKEN_INVALID}。
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT 已过期：{}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 解析失败：{}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
    }

    /** 从 claims 还原登录用户 */
    public LoginUser toLoginUser(Claims claims) {
        Long userId = claims.get("userId", Number.class).longValue();
        String role = claims.get("role", String.class);
        return new LoginUser(userId, claims.getSubject(), role);
    }

    public Long getAccessTokenExpireSec() {
        return jwtProperties.getAccessTokenExpireSec();
    }
}
