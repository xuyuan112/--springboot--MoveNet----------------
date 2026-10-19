package com.rehab.common.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 登录用户主体：解析 JWT 后放入 SecurityContext，
 * 业务层通过 {@link SecurityUtils} 获取当前用户，禁止从前端入参取患者/治疗师 ID。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements UserDetails {

    /** 用户账号 ID（user_account.user_id） */
    private Long userId;

    /** 登录名 */
    private String username;

    /** 角色编码：PATIENT / THERAPIST / ADMIN */
    private String roleCode;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }

    /** JWT 无状态认证，不持有密码 */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 账号启用状态在登录时校验；令牌存续期内禁用由会话 Redis 键删除实现强踢 */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
