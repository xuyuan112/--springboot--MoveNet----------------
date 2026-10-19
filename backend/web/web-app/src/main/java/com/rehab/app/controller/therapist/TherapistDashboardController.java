package com.rehab.app.controller.therapist;

import com.rehab.app.service.TherapistDashboardService;
import com.rehab.app.vo.TherapistDashboardVO;
import com.rehab.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 治疗师端工作台接口（P6-B1）。
 */
@Tag(name = "治疗师端-工作台")
@RestController
@RequestMapping("/therapist")
@RequiredArgsConstructor
public class TherapistDashboardController {

    private final TherapistDashboardService therapistDashboardService;

    @Operation(summary = "工作台聚合指标（负责患者数/今日完成率/待审核报告/待回复咨询/今日低分训练）")
    @GetMapping("/dashboard")
    public ApiResult<TherapistDashboardVO> dashboard() {
        return ApiResult.ok(therapistDashboardService.getDashboard());
    }
}
