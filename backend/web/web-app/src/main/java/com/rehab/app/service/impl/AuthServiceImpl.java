package com.rehab.app.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rehab.model.mapper.account.PatientProfileMapper;
import com.rehab.model.mapper.account.TherapistProfileMapper;
import com.rehab.model.mapper.account.UserAccountMapper;
import com.rehab.app.service.AuthService;
import com.rehab.app.service.CaptchaService;
import com.rehab.common.constant.Constants;
import com.rehab.common.constant.RedisKeyConstants;
import com.rehab.common.event.OperationLogEvent;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.JwtTokenProvider;
import com.rehab.common.security.LoginUser;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.dto.LoginRequest;
import com.rehab.model.dto.PasswordChangeRequest;
import com.rehab.model.entity.PatientProfile;
import com.rehab.model.entity.TherapistProfile;
import com.rehab.model.entity.UserAccount;
import com.rehab.model.enums.OperationStatus;
import com.rehab.model.enums.RoleCode;
import com.rehab.app.vo.LoginVO;
import com.rehab.app.vo.UserInfoVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 认证服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountMapper userAccountMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final TherapistProfileMapper therapistProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final CaptchaService captchaService;

    @Override
    public LoginVO login(LoginRequest request, String ip, String deviceId) {
        // 0. 先校验图形验证码（一次性，校验通过/失败都立即从 Redis 删除）
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());

        String username = request.getUsername().trim();

        // 1. 账号是否处于锁定中
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.LOGIN_LOCK + username))) {
            publishAuthEvent(null, "LOGIN", OperationStatus.FAIL.getCode(), "账号处于锁定窗口期", ip, deviceId);
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED);
        }

        // 2. 查询账号（逻辑删除自动过滤）
        UserAccount user = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUsername, username));

        // 3. 密码校验（用户不存在时以相同错误提示，避免账号枚举）
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleLoginFailure(user, username, ip, deviceId);
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 4. 账号状态校验
        if (!Constants.ENABLED.equals(user.getStatus())) {
            publishAuthEvent(user.getUserId(), "LOGIN", OperationStatus.FAIL.getCode(), "账号已禁用", ip, deviceId);
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 5. 失败计数清零
        stringRedisTemplate.delete(RedisKeyConstants.LOGIN_FAIL_COUNT + username);

        // 6. 签发令牌并写入 Redis 会话（jti 用于登出失效/禁用强踢）
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getUsername(), user.getRoleCode());
        String token = jwtTokenProvider.createAccessToken(loginUser);
        Claims claims = jwtTokenProvider.parse(token);
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.JWT_SESSION + user.getUserId(),
                claims.getId(),
                Duration.ofSeconds(jwtTokenProvider.getAccessTokenExpireSec()));

        // 7. 更新最后登录时间（只更新单字段）
        userAccountMapper.update(null, new LambdaUpdateWrapper<UserAccount>()
                .eq(UserAccount::getUserId, user.getUserId())
                .set(UserAccount::getLastLoginAt, LocalDateTime.now()));

        // 8. 登录审计日志
        publishAuthEvent(user.getUserId(), "LOGIN", OperationStatus.SUCCESS.getCode(), null, ip, deviceId);

        return LoginVO.builder()
                .accessToken(token)
                .expiresIn(jwtTokenProvider.getAccessTokenExpireSec())
                .user(buildUserInfo(user))
                .build();
    }

    @Override
    public void logout() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        stringRedisTemplate.delete(RedisKeyConstants.JWT_SESSION + loginUser.getUserId());
        publishAuthEvent(loginUser.getUserId(), "LOGOUT", OperationStatus.SUCCESS.getCode(), null,
                com.rehab.common.util.ServletUtils.currentIp(),
                com.rehab.common.util.ServletUtils.currentDeviceId());
    }

    @Override
    public UserInfoVO currentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        UserAccount user = userAccountMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.AUTH_REQUIRED);
        }
        return buildUserInfo(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(PasswordChangeRequest request) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        UserAccount user = userAccountMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在或已删除");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "原密码不正确");
        }
        userAccountMapper.update(null, new LambdaUpdateWrapper<UserAccount>()
                .eq(UserAccount::getUserId, user.getUserId())
                .set(UserAccount::getPasswordHash, passwordEncoder.encode(request.getNewPassword())));
        // 密码修改后使所有会话失效，前端引导重新登录
        stringRedisTemplate.delete(RedisKeyConstants.JWT_SESSION + user.getUserId());
    }

    /**
     * 登录失败处理：计数 +1，达到 5 次写入锁定键（30 分钟）。
     */
    private void handleLoginFailure(UserAccount user, String username, String ip, String deviceId) {
        // 账号存在时失败日志关联其 userId，便于管理端按用户检索；账号不存在时为 null
        Long userId = user != null ? user.getUserId() : null;
        String failKey = RedisKeyConstants.LOGIN_FAIL_COUNT + username;
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, Duration.ofMinutes(30));
        }
        if (count != null && count >= Constants.LOGIN_FAIL_LIMIT) {
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.LOGIN_LOCK + username, "1",
                    Duration.ofMinutes(Constants.LOGIN_LOCK_MINUTES));
            stringRedisTemplate.delete(failKey);
            publishAuthEvent(userId, "LOGIN", OperationStatus.FAIL.getCode(), "连续失败达到上限，账号锁定", ip, deviceId);
        } else {
            publishAuthEvent(userId, "LOGIN", OperationStatus.FAIL.getCode(),
                    "用户名或密码错误，第 " + count + " 次", ip, deviceId);
        }
    }

    /**
     * 组装用户信息（姓名按角色取档案真实名，缺省回退登录名）。
     */
    private UserInfoVO buildUserInfo(UserAccount user) {
        String name = user.getUsername();
        if (RoleCode.PATIENT.getCode().equals(user.getRoleCode())) {
            PatientProfile profile = patientProfileMapper.selectOne(
                    new LambdaQueryWrapper<PatientProfile>()
                            .eq(PatientProfile::getUserId, user.getUserId())
                            .last("limit 1"));
            if (profile != null && StringUtils.hasText(profile.getRealName())) {
                name = profile.getRealName();
            }
        } else if (RoleCode.THERAPIST.getCode().equals(user.getRoleCode())) {
            TherapistProfile profile = therapistProfileMapper.selectOne(
                    new LambdaQueryWrapper<TherapistProfile>()
                            .eq(TherapistProfile::getUserId, user.getUserId())
                            .last("limit 1"));
            if (profile != null && StringUtils.hasText(profile.getRealName())) {
                name = profile.getRealName();
            }
        }
        return UserInfoVO.builder()
                .id(user.getUserId())
                .role(user.getRoleCode())
                .name(name)
                .build();
    }

    /**
     * 发布认证审计事件（登录时 SecurityContext 尚不存在，手动携带 userId/ip/device）。
     */
    private void publishAuthEvent(Long userId, String action, String status,
                                  String message, String ip, String deviceId) {
        eventPublisher.publishEvent(OperationLogEvent.builder()
                .userId(userId)
                .module("AUTH")
                .action(action)
                .targetType("USER_ACCOUNT")
                .ip(StringUtils.hasText(ip) ? ip : "unknown")
                .deviceId(deviceId)
                .status(status)
                .detail(StringUtils.hasText(message) ? JSONUtil.createObj().set("message", message).toString() : null)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
