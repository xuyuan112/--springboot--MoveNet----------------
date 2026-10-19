package com.rehab.common.security;

import com.rehab.common.config.JwtProperties;
import com.rehab.common.constant.RedisKeyConstants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：
 * <ol>
 *   <li>从 Authorization 头提取 Bearer 令牌；</li>
 *   <li>验签并解析 userId/role/jti；</li>
 *   <li>与 Redis 会话键 {@code jwt:user:{userId}} 中的 jti 比对（支持登出失效与禁用强踢）；</li>
 *   <li>通过后写入 SecurityContext。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtTokenProvider.parse(token);
                LoginUser loginUser = jwtTokenProvider.toLoginUser(claims);

                // Redis 会话校验：登出 / 禁用账号后会话键被删除，令牌即时失效
                String sessionJti = stringRedisTemplate.opsForValue()
                        .get(RedisKeyConstants.JWT_SESSION + loginUser.getUserId());
                if (claims.getId() != null && claims.getId().equals(sessionJti)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // 解析失败不直接写响应，交给 AuthenticationEntryPoint 统一返回 401
                log.trace("JWT 认证未通过：{}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(jwtProperties.getTokenPrefix())) {
            return header.substring(jwtProperties.getTokenPrefix().length()).trim();
        }
        return null;
    }
}
