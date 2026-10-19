package com.rehab.app.controller;

import com.rehab.app.service.AuthService;
import com.rehab.app.service.CaptchaService;
import com.rehab.common.result.ApiResult;
import com.rehab.common.util.ServletUtils;
import com.rehab.model.dto.LoginRequest;
import com.rehab.model.dto.PasswordChangeRequest;
import com.rehab.app.vo.CaptchaVO;
import com.rehab.app.vo.LoginVO;
import com.rehab.app.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（三端共用）：/auth/**
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @Operation(summary = "获取登录图形验证码")
    @GetMapping("/captcha")
    public ApiResult<CaptchaVO> captcha() {
        return ApiResult.ok(captchaService.generate());
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        // 登录日志在 Service 内发布（此时尚无 SecurityContext，需手动携带 IP/设备）
        return ApiResult.ok(authService.login(request,
                ServletUtils.currentIp(), ServletUtils.currentDeviceId()));
    }

    @Operation(summary = "退出登录（令牌即时失效）")
    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        authService.logout();
        return ApiResult.ok();
    }

    @Operation(summary = "查询当前登录用户")
    @GetMapping("/me")
    public ApiResult<UserInfoVO> me() {
        return ApiResult.ok(authService.currentUser());
    }

    @Operation(summary = "修改密码（成功后需重新登录）")
    @PutMapping("/password")
    public ApiResult<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request);
        return ApiResult.ok();
    }
}
