package com.rehab.app.controller.therapist;

import com.rehab.app.service.TherapistPatientService;
import com.rehab.app.service.TherapistPlanService;
import com.rehab.app.vo.TherapistAlertVO;
import com.rehab.app.vo.TherapistPatientDetailVO;
import com.rehab.app.vo.TherapistPatientTaskVO;
import com.rehab.app.vo.TherapistPatientVO;
import com.rehab.app.vo.TherapistPlanVO;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 治疗师端患者管理接口（P6-B2/P6-B4，接口文档 5.1）。
 *
 * <p>全部查询类接口；患者维度数据访问由 Service 层校验 ACTIVE 绑定（违例 403）。
 * 患者计划列表复用计划服务的患者维度查询（同样校验绑定）。</p>
 */
@Tag(name = "治疗师端-患者管理")
@RestController
@RequestMapping("/therapist/patients")
@RequiredArgsConstructor
@Validated
public class TherapistPatientController {

    private final TherapistPatientService therapistPatientService;
    private final TherapistPlanService therapistPlanService;

    @Operation(summary = "分页查询当前有效绑定患者")
    @GetMapping
    public ApiResult<PageResult<TherapistPatientVO>> listPatients(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String planStatus) {
        return ApiResult.ok(therapistPatientService.listPatients(page, size, keyword, partCode, planStatus));
    }

    @Operation(summary = "患者详情（档案+绑定信息+风险提示，敏感字段脱敏）")
    @GetMapping("/{patientId}")
    public ApiResult<TherapistPatientDetailVO> getPatientDetail(@PathVariable Long patientId) {
        return ApiResult.ok(therapistPatientService.getPatientDetail(patientId));
    }

    @Operation(summary = "分页查询患者计划（本人创建，可按状态过滤）")
    @GetMapping("/{patientId}/plans")
    public ApiResult<PageResult<TherapistPlanVO>> listPatientPlans(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(therapistPlanService.listPatientPlans(patientId, status, page, size));
    }

    @Operation(summary = "分页查询患者每日任务（仅本人创建计划产生的任务）")
    @GetMapping("/{patientId}/tasks")
    public ApiResult<PageResult<TherapistPatientTaskVO>> listPatientTasks(
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return ApiResult.ok(therapistPatientService.listPatientTasks(patientId, from, to, status, page, size));
    }

    @Operation(summary = "查询患者训练异常提示（近 7 日窗口，仅提示不构成医学诊断）")
    @GetMapping("/{patientId}/alerts")
    public ApiResult<PageResult<TherapistAlertVO>> listPatientAlerts(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return ApiResult.ok(therapistPatientService.listPatientAlerts(patientId, page, size));
    }
}
