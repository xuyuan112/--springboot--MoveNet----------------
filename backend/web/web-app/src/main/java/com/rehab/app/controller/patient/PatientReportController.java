package com.rehab.app.controller.patient;

import com.rehab.app.service.PatientReportService;
import com.rehab.app.vo.PatientReportDetailVO;
import com.rehab.app.vo.PatientReportVO;
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

/**
 * 患者端评估报告接口（P5-B2）：/patient/reports。
 *
 * <p>患者只读：仅 PUBLISHED（接口文档 EFFECTIVE）可见，其余状态 404；
 * 不提供任何修改报告的接口。</p>
 */
@Tag(name = "患者端 - 评估报告")
@Validated
@RestController
@RequestMapping("/patient/reports")
@RequiredArgsConstructor
public class PatientReportController {

    private final PatientReportService patientReportService;

    @Operation(summary = "分页查询本人有效评估报告")
    @GetMapping
    public ApiResult<PageResult<PatientReportVO>> pageMyReports(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(patientReportService.pageMyReports(page, size, status));
    }

    @Operation(summary = "查询报告正文、统计数据与免责声明")
    @GetMapping("/{reportId}")
    public ApiResult<PatientReportDetailVO> getMyReport(@PathVariable Long reportId) {
        return ApiResult.ok(patientReportService.getMyReport(reportId));
    }
}
