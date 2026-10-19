package com.rehab.app.controller.therapist;

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
 * 治疗师端接口：/therapist/**（仅 THERAPIST 角色）。
 * 后续业务模块：工作台、患者管理、计划制定、训练数据、报告审核、咨询回复。
 * 所有患者数据访问前必须在 Service 层校验有效 patient_therapist 绑定（对象级权限）。
 */
@Tag(name = "治疗师端接口")
@RestController
@RequestMapping("/therapist")
@RequiredArgsConstructor
public class TherapistController {

    private final AuthService authService;

    @Operation(summary = "当前治疗师信息（命名空间连通性示例）")
    @GetMapping("/me")
    public ApiResult<UserInfoVO> me() {
        return ApiResult.ok(authService.currentUser());
    }
}
