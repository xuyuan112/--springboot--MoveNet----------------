package com.rehab.app.controller.therapist;

import com.rehab.app.service.TherapistPlanService;
import com.rehab.app.vo.TherapistPlanDetailVO;
import com.rehab.app.vo.TherapistPlanItemVO;
import com.rehab.app.vo.TherapistPlanVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.PlanCreateRequest;
import com.rehab.model.dto.PlanItemRequest;
import com.rehab.model.dto.PlanScheduleRequest;
import com.rehab.model.dto.PlanUpdateRequest;
import com.rehab.model.dto.ReasonRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 治疗师端康复计划接口（P6-B3/P6-B4，接口文档 5.2）。
 *
 * <p>状态机 DRAFT→PUBLISHED→PAUSED/ARCHIVED；仅 DRAFT 可编辑/删除；
 * 写操作全部 @OperationLog(module="PLAN") 并由 Service 层保证幂等与归属校验。</p>
 */
@Tag(name = "治疗师端-康复计划")
@RestController
@RequestMapping("/therapist")
@RequiredArgsConstructor
@Validated
public class TherapistPlanController {

    private final TherapistPlanService therapistPlanService;

    // ==================== 计划 CRUD ====================

    @Operation(summary = "创建 DRAFT 计划（校验患者绑定，含动作项）")
    @PostMapping("/patients/{patientId}/plans")
    @OperationLog(module = "PLAN", action = "CREATE_PLAN", targetType = "REHAB_PLAN", recordParam = false)
    public ApiResult<TherapistPlanDetailVO> createPlan(@PathVariable Long patientId,
                                                       @Valid @RequestBody PlanCreateRequest request) {
        return ApiResult.ok(therapistPlanService.createPlan(patientId, request));
    }

    @Operation(summary = "计划详情（基本信息+动作项+执行日程）")
    @GetMapping("/plans/{planId}")
    public ApiResult<TherapistPlanDetailVO> getPlanDetail(@PathVariable Long planId) {
        return ApiResult.ok(therapistPlanService.getPlanDetail(planId));
    }

    @Operation(summary = "修改草稿计划基本信息（仅 DRAFT）")
    @PutMapping("/plans/{planId}")
    @OperationLog(module = "PLAN", action = "UPDATE_PLAN", targetType = "REHAB_PLAN", recordParam = true)
    public ApiResult<TherapistPlanDetailVO> updatePlan(@PathVariable Long planId,
                                                       @Valid @RequestBody PlanUpdateRequest request) {
        return ApiResult.ok(therapistPlanService.updatePlan(planId, request));
    }

    @Operation(summary = "删除草稿计划（仅 DRAFT）")
    @DeleteMapping("/plans/{planId}")
    @OperationLog(module = "PLAN", action = "DELETE_PLAN", targetType = "REHAB_PLAN", recordParam = true)
    public ApiResult<Void> deleteDraftPlan(@PathVariable Long planId) {
        therapistPlanService.deleteDraftPlan(planId);
        return ApiResult.ok();
    }

    // ==================== 状态机 ====================

    @Operation(summary = "发布计划（DRAFT→PUBLISHED，校验动作版本/参数范围/日期）")
    @PostMapping("/plans/{planId}/publish")
    @OperationLog(module = "PLAN", action = "PUBLISH_PLAN", targetType = "REHAB_PLAN", recordParam = true)
    public ApiResult<TherapistPlanDetailVO> publishPlan(@PathVariable Long planId) {
        return ApiResult.ok(therapistPlanService.publishPlan(planId));
    }

    @Operation(summary = "暂停计划（PUBLISHED→PAUSED，需原因）")
    @PostMapping("/plans/{planId}/pause")
    @OperationLog(module = "PLAN", action = "PAUSE_PLAN", targetType = "REHAB_PLAN", recordParam = true)
    public ApiResult<Void> pausePlan(@PathVariable Long planId, @Valid @RequestBody ReasonRequest request) {
        therapistPlanService.pausePlan(planId, request);
        return ApiResult.ok();
    }

    @Operation(summary = "恢复计划（PAUSED→PUBLISHED）")
    @PostMapping("/plans/{planId}/resume")
    @OperationLog(module = "PLAN", action = "RESUME_PLAN", targetType = "REHAB_PLAN", recordParam = true)
    public ApiResult<Void> resumePlan(@PathVariable Long planId) {
        therapistPlanService.resumePlan(planId);
        return ApiResult.ok();
    }

    @Operation(summary = "归档计划（PUBLISHED/PAUSED→ARCHIVED，停止生成新任务，需原因）")
    @PostMapping("/plans/{planId}/archive")
    @OperationLog(module = "PLAN", action = "ARCHIVE_PLAN", targetType = "REHAB_PLAN", recordParam = true)
    public ApiResult<Void> archivePlan(@PathVariable Long planId, @Valid @RequestBody ReasonRequest request) {
        therapistPlanService.archivePlan(planId, request);
        return ApiResult.ok();
    }

    // ==================== 版本与动作项 ====================

    @Operation(summary = "版本复制（复制当前计划创建新 DRAFT 版本）")
    @PostMapping("/plans/{planId}/versions")
    @OperationLog(module = "PLAN", action = "COPY_PLAN_VERSION", targetType = "REHAB_PLAN", recordParam = true)
    public ApiResult<TherapistPlanDetailVO> createNewVersion(@PathVariable Long planId) {
        return ApiResult.ok(therapistPlanService.createNewVersion(planId));
    }

    @Operation(summary = "分页查询同名计划版本链")
    @GetMapping("/plans/{planId}/versions")
    public ApiResult<PageResult<TherapistPlanVO>> listVersions(
            @PathVariable Long planId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return ApiResult.ok(therapistPlanService.listVersions(planId, page, size));
    }

    @Operation(summary = "分页查询计划动作项")
    @GetMapping("/plans/{planId}/items")
    public ApiResult<PageResult<TherapistPlanItemVO>> listItems(
            @PathVariable Long planId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return ApiResult.ok(therapistPlanService.listItems(planId, page, size));
    }

    @Operation(summary = "新增草稿动作项（校验动作版本与参数范围）")
    @PostMapping("/plans/{planId}/items")
    @OperationLog(module = "PLAN", action = "ADD_PLAN_ITEM", targetType = "PLAN_ITEM", recordParam = false)
    public ApiResult<TherapistPlanItemVO> addPlanItem(@PathVariable Long planId,
                                                      @Valid @RequestBody PlanItemRequest request) {
        return ApiResult.ok(therapistPlanService.addPlanItem(planId, request));
    }

    @Operation(summary = "修改草稿动作项")
    @PutMapping("/plans/{planId}/items/{itemId}")
    @OperationLog(module = "PLAN", action = "UPDATE_PLAN_ITEM", targetType = "PLAN_ITEM", recordParam = true)
    public ApiResult<TherapistPlanItemVO> updatePlanItem(@PathVariable Long planId, @PathVariable Long itemId,
                                                         @Valid @RequestBody PlanItemRequest request) {
        return ApiResult.ok(therapistPlanService.updatePlanItem(planId, itemId, request));
    }

    @Operation(summary = "删除草稿动作项")
    @DeleteMapping("/plans/{planId}/items/{itemId}")
    @OperationLog(module = "PLAN", action = "DELETE_PLAN_ITEM", targetType = "PLAN_ITEM", recordParam = true)
    public ApiResult<Void> deletePlanItem(@PathVariable Long planId, @PathVariable Long itemId) {
        therapistPlanService.deletePlanItem(planId, itemId);
        return ApiResult.ok();
    }

    @Operation(summary = "设置执行日程（DAILY/WEEKLY 频率+执行星期，仅 DRAFT）")
    @PutMapping("/plans/{planId}/schedule")
    @OperationLog(module = "PLAN", action = "UPDATE_PLAN_SCHEDULE", targetType = "REHAB_PLAN", recordParam = true)
    public ApiResult<TherapistPlanDetailVO> updateSchedule(@PathVariable Long planId,
                                                           @Valid @RequestBody PlanScheduleRequest request) {
        return ApiResult.ok(therapistPlanService.updateSchedule(planId, request));
    }
}
