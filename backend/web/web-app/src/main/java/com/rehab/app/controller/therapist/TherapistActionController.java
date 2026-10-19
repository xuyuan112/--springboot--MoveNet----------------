package com.rehab.app.controller.therapist;

import com.rehab.app.service.PatientActionService;
import com.rehab.app.vo.PatientActionVO;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 治疗师端动作库接口：计划编辑器选择动作用，仅 PUBLISHED 动作
 * （复用患者端动作库查询逻辑，查询口径一致）。
 */
@Tag(name = "治疗师端-动作库")
@RestController
@RequestMapping("/therapist/actions")
@RequiredArgsConstructor
@Validated
public class TherapistActionController {

    private final PatientActionService patientActionService;

    @Operation(summary = "分页浏览已发布动作（partCode/difficulty/keyword 筛选）")
    @GetMapping
    public ApiResult<PageResult<PatientActionVO>> listActions(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(patientActionService.listActions(page, size, partCode, difficulty, keyword));
    }
}
