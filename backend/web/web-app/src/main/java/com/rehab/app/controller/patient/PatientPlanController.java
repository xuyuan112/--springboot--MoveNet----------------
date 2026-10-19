package com.rehab.app.controller.patient;

import com.rehab.app.service.PatientPlanService;
import com.rehab.app.vo.PatientPlanItemVO;
import com.rehab.app.vo.PatientPlanVO;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 患者端康复计划接口：/patient/plans/**。
 */
@Tag(name = "患者端 - 康复计划")
@Validated
@RestController
@RequestMapping("/patient/plans")
@RequiredArgsConstructor
public class PatientPlanController {

    private final PatientPlanService patientPlanService;

    @Operation(summary = "分页查询本人计划")
    @GetMapping
    public ApiResult<PageResult<PatientPlanVO>> listPlans(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(patientPlanService.listMyPlans(page, size, status));
    }

    @Operation(summary = "查询计划详情")
    @GetMapping("/{planId}")
    public ApiResult<PatientPlanVO> getPlanDetail(@PathVariable Long planId) {
        return ApiResult.ok(patientPlanService.getMyPlanDetail(planId));
    }

    @Operation(summary = "查询计划动作项及目标参数")
    @GetMapping("/{planId}/items")
    public ApiResult<List<PatientPlanItemVO>> listPlanItems(@PathVariable Long planId) {
        return ApiResult.ok(patientPlanService.listMyPlanItems(planId));
    }
}
