package com.rehab.app.controller.patient;

import com.rehab.app.service.PatientProgressService;
import com.rehab.app.vo.PatientProgressPageVO;
import com.rehab.app.vo.PatientTrendPointVO;
import com.rehab.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 患者端进度与趋势接口（P5-B1）：/patient/progress、/patient/trends。
 *
 * <p>完成度与趋势均排除 INSUFFICIENT 记录；日级聚合走 Redis 今日缓存（写后失效）。</p>
 */
@Tag(name = "患者端 - 进度与趋势")
@Validated
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientProgressController {

    private final PatientProgressService patientProgressService;

    @Operation(summary = "三级完成度（日/计划/动作项）+ 任务完成明细分页")
    @GetMapping("/progress")
    public ApiResult<PatientProgressPageVO> getProgress(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long planId) {
        return ApiResult.ok(patientProgressService.getProgress(page, size, from, to, planId));
    }

    @Operation(summary = "得分/达标率/训练次数趋势（排除 INSUFFICIENT）")
    @GetMapping("/trends")
    public ApiResult<List<PatientTrendPointVO>> getTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long actionVersionId,
            @RequestParam(defaultValue = "DAY") String granularity) {
        return ApiResult.ok(patientProgressService.getTrends(from, to, actionVersionId, granularity));
    }
}
