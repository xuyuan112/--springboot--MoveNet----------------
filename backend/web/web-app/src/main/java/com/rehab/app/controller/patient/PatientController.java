package com.rehab.app.controller.patient;

import com.rehab.app.service.AuthService;
import com.rehab.common.result.ApiResult;
import com.rehab.app.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者端接口：/patient/**（仅 PATIENT 角色，由 SecurityConfig 统一拦截）。
 * 后续业务模块：我的资料、康复计划、今日任务、训练记录、趋势报告、内容收藏、咨询。
 */
@Tag(name = "患者端接口")
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private final AuthService authService;

    @Operation(summary = "当前患者信息（命名空间连通性示例）")
    @GetMapping("/me")
    public ApiResult<UserInfoVO> me() {
        return ApiResult.ok(authService.currentUser());
    }
}
