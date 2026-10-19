package com.rehab.admin.controller.account;

import com.rehab.admin.service.AdminUserService;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.AdminUserCreateRequest;
import com.rehab.model.dto.AdminUserUpdateRequest;
import com.rehab.model.dto.ReasonRequest;
import com.rehab.admin.vo.AdminUserCreateVO;
import com.rehab.admin.vo.AdminUserDetailVO;
import com.rehab.admin.vo.AdminUserVO;
import com.rehab.admin.vo.LoginLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 账号管理（/admin/users，仅 ADMIN）。
 * 患者/治疗师账号的创建、查询、启停、重置密码与登录日志。
 */
@Tag(name = "管理端-账号管理")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "分页查询账号（角色/状态/关键词）")
    @GetMapping
    public ApiResult<PageResult<AdminUserVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(adminUserService.pageUsers(page, size, role, status, keyword));
    }

    @Operation(summary = "创建患者/治疗师账号（同步建立角色档案）")
    @OperationLog(module = "USER", action = "CREATE", targetType = "USER_ACCOUNT", recordParam = true)
    @PostMapping
    public ApiResult<AdminUserCreateVO> create(@Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResult.ok(adminUserService.createUser(request));
    }

    @Operation(summary = "查询账号及角色档案详情")
    @GetMapping("/{userId}")
    public ApiResult<AdminUserDetailVO> detail(@PathVariable Long userId) {
        return ApiResult.ok(adminUserService.detail(userId));
    }

    @Operation(summary = "修改账号非敏感资料")
    @OperationLog(module = "USER", action = "UPDATE", targetType = "USER_ACCOUNT", recordParam = true)
    @PutMapping("/{userId}")
    public ApiResult<Void> update(@PathVariable Long userId,
                                  @Valid @RequestBody AdminUserUpdateRequest request) {
        adminUserService.updateUser(userId, request);
        return ApiResult.ok();
    }

    @Operation(summary = "启用账号")
    @OperationLog(module = "USER", action = "ENABLE", targetType = "USER_ACCOUNT")
    @PostMapping("/{userId}/enable")
    public ApiResult<Void> enable(@PathVariable Long userId) {
        adminUserService.enable(userId);
        return ApiResult.ok();
    }

    @Operation(summary = "禁用账号（令牌立即失效，需填写原因）")
    @OperationLog(module = "USER", action = "DISABLE", targetType = "USER_ACCOUNT", recordParam = true)
    @PostMapping("/{userId}/disable")
    public ApiResult<Void> disable(@PathVariable Long userId,
                                   @Valid @RequestBody ReasonRequest reason) {
        adminUserService.disable(userId, reason);
        return ApiResult.ok();
    }

    @Operation(summary = "重置密码（重置为平台初始密码，不返回明文）")
    @OperationLog(module = "USER", action = "RESET_PASSWORD", targetType = "USER_ACCOUNT")
    @PostMapping("/{userId}/reset-password")
    public ApiResult<Void> resetPassword(@PathVariable Long userId) {
        adminUserService.resetPassword(userId);
        return ApiResult.ok();
    }

    @Operation(summary = "分页查询账号登录日志（AUTH 审计）")
    @GetMapping("/{userId}/login-logs")
    public ApiResult<PageResult<LoginLogVO>> loginLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Boolean success) {
        return ApiResult.ok(adminUserService.pageLoginLogs(userId, page, size, success));
    }
}
