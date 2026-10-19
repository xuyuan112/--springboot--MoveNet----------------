package com.rehab.app.controller.therapist;

import com.rehab.app.service.TherapistReportService;
import com.rehab.app.vo.TherapistReportDetailVO;
import com.rehab.app.vo.TherapistReportVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ReportReviewRequest;
import com.rehab.model.dto.ReportSubmitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 治疗师端评估报告接口（P7-B4，接口文档 5.4）。
 *
 * <p>状态机（枚举映射）：DRAFT/AI_GENERATED --submit--> REVIEWED --review:APPROVE--> PUBLISHED
 * （即文档语义 EFFECTIVE，患者端可见），--review:REJECT--> DRAFT（comment 必填）。
 * PUBLISHED 后不可再提交；regenerate 产生新 DRAFT 版本、原版本保留；
 * 归属经 plan.therapist_id 校验（非本人 403）。</p>
 */
@Tag(name = "治疗师端-评估报告")
@RestController
@RequestMapping("/therapist")
@RequiredArgsConstructor
@Validated
public class TherapistReportController {

    private final TherapistReportService therapistReportService;

    @Operation(summary = "分页查询负责患者评估报告（patientId/planId/status 过滤）")
    @GetMapping("/reports")
    public ApiResult<PageResult<TherapistReportVO>> listReports(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(therapistReportService.listReports(page, size, patientId, planId, status));
    }

    @Operation(summary = "报告详情（正文+data_snapshot+AI 草稿+结论+免责声明）")
    @GetMapping("/reports/{reportId}")
    public ApiResult<TherapistReportDetailVO> getReportDetail(@PathVariable Long reportId) {
        return ApiResult.ok(therapistReportService.getReportDetail(reportId));
    }

    @Operation(summary = "补充结论并提交（DRAFT/AI_GENERATED→REVIEWED；PUBLISHED 409）")
    @PostMapping("/reports/{reportId}/submit")
    @OperationLog(module = "REPORT", action = "SUBMIT_REPORT", targetType = "ASSESSMENT_REPORT", recordParam = false)
    public ApiResult<TherapistReportDetailVO> submit(@PathVariable Long reportId,
                                                     @Valid @RequestBody ReportSubmitRequest request) {
        return ApiResult.ok(therapistReportService.submit(reportId, request));
    }

    @Operation(summary = "审核（APPROVE→PUBLISHED；REJECT→DRAFT，comment 必填）")
    @PostMapping("/reports/{reportId}/review")
    @OperationLog(module = "REPORT", action = "REVIEW_REPORT", targetType = "ASSESSMENT_REPORT", recordParam = false)
    public ApiResult<TherapistReportDetailVO> review(@PathVariable Long reportId,
                                                     @Valid @RequestBody ReportReviewRequest request) {
        return ApiResult.ok(therapistReportService.review(reportId, request));
    }

    @Operation(summary = "按冻结数据重新生成新 DRAFT 版本（原版本保留）")
    @PostMapping("/reports/{reportId}/regenerate")
    @OperationLog(module = "REPORT", action = "REGENERATE_REPORT", targetType = "ASSESSMENT_REPORT", recordParam = false)
    public ApiResult<TherapistReportDetailVO> regenerate(@PathVariable Long reportId) {
        return ApiResult.ok(therapistReportService.regenerate(reportId));
    }

    @Operation(summary = "分页查询同计划同周期报告版本链（版本号倒序）")
    @GetMapping("/reports/{reportId}/versions")
    public ApiResult<PageResult<TherapistReportVO>> listVersions(
            @PathVariable Long reportId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return ApiResult.ok(therapistReportService.listVersions(reportId, page, size));
    }
}
