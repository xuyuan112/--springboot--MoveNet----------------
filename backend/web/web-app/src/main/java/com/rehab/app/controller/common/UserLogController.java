package com.rehab.app.controller.common;

import com.rehab.app.service.UserLoginLogService;
import com.rehab.app.vo.LoginLogVO;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本人登录日志查询接口（三端共用）：/users/me/login-logs。
 * 仅认证即可访问，不限制角色（SecurityConfig 中走 anyRequest.authenticated）。
 */
@Tag(name = "公共 - 登录日志")
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserLogController {

    private final UserLoginLogService userLoginLogService;

    @Operation(summary = "分页查询本人登录日志（module='AUTH'）")
    @GetMapping("/login-logs")
    public ApiResult<PageResult<LoginLogVO>> pageMyLoginLogs(
            @Parameter(description = "页码（从 1 开始）") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer size) {
        return ApiResult.ok(userLoginLogService.pageMyLoginLogs(page, size));
    }
}
