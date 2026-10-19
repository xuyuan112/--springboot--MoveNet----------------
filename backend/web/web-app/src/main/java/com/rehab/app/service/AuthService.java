package com.rehab.app.service;

import com.rehab.model.dto.LoginRequest;
import com.rehab.model.dto.PasswordChangeRequest;
import com.rehab.app.vo.LoginVO;
import com.rehab.app.vo.UserInfoVO;

/**
 * 认证服务：登录、登出、当前用户、修改密码。
 */
public interface AuthService {

    /**
     * 账号密码登录（BCrypt 校验、失败 5 次锁定 30 分钟、签发 JWT 并写入 Redis 会话）。
     */
    LoginVO login(LoginRequest request, String ip, String deviceId);

    /**
     * 登出：删除 Redis 会话，当前令牌即时失效。
     */
    void logout();

    /**
     * 获取当前登录用户信息。
     */
    UserInfoVO currentUser();

    /**
     * 修改密码：校验原密码后更新，并删除当前会话（需重新登录）。
     */
    void changePassword(PasswordChangeRequest request);
}
