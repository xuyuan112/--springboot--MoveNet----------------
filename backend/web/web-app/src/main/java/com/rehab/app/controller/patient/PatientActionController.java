package com.rehab.app.controller.patient;

import com.rehab.app.service.PatientActionService;
import com.rehab.app.vo.PatientActionDetailVO;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者端动作库接口（P4-B1/P4-B3）：/patient/actions/**。
 *
 * <p>仅展示 PUBLISHED 动作及 PUBLISHED 版本快照（只读）；
 * 详情下发 algo_config / motion_signature / 评估规则 / pose_engine_id。</p>
 */
@Tag(name = "患者端 - 动作库")
@Validated
@RestController
@RequestMapping("/patient/actions")
@RequiredArgsConstructor
public class PatientActionController {

    private final PatientActionService patientActionService;

    @Operation(summary = "分页浏览已发布动作（部位/难度/关键词筛选）")
    @GetMapping
    public ApiResult<PageResult<PatientActionVO>> listActions(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(patientActionService.listActions(page, size, partCode, difficulty, keyword));
    }

    @Operation(summary = "查询动作版本快照详情（只读）")
    @GetMapping("/{actionVersionId}")
    public ApiResult<PatientActionDetailVO> getActionDetail(@PathVariable Long actionVersionId) {
        return ApiResult.ok(patientActionService.getActionDetail(actionVersionId));
    }
}
