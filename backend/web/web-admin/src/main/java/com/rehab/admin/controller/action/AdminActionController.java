package com.rehab.admin.controller.action;

import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ActionSaveRequest;
import com.rehab.admin.service.AdminActionService;
import com.rehab.admin.vo.AdminActionDetailVO;
import com.rehab.admin.vo.AdminActionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 动作基础信息管理：/admin/actions（仅 ADMIN）。
 */
@Tag(name = "管理端-动作管理")
@RestController
@RequestMapping("/admin/actions")
@RequiredArgsConstructor
public class AdminActionController {

    private final AdminActionService adminActionService;

    @Operation(summary = "分页查询动作")
    @GetMapping
    public ApiResult<PageResult<AdminActionVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String targetPart,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(adminActionService.pageActions(page, size, targetPart, status, keyword));
    }

    @Operation(summary = "创建动作")
    @OperationLog(module = "ACTION", action = "CREATE", targetType = "ACTION", recordParam = true)
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody ActionSaveRequest request) {
        Long actionId = adminActionService.createAction(request);
        return ApiResult.ok(actionId);
    }

    @Operation(summary = "查询动作详情及版本列表")
    @GetMapping("/{actionId}")
    public ApiResult<AdminActionDetailVO> detail(@PathVariable Long actionId) {
        return ApiResult.ok(adminActionService.detail(actionId));
    }

    @Operation(summary = "修改动作基础信息（仅草稿可编辑）")
    @OperationLog(module = "ACTION", action = "UPDATE", targetType = "ACTION", recordParam = true)
    @PutMapping("/{actionId}")
    public ApiResult<Void> update(@PathVariable Long actionId,
                                  @Valid @RequestBody ActionSaveRequest request) {
        adminActionService.updateAction(actionId, request);
        return ApiResult.ok();
    }

    @Operation(summary = "动作下线")
    @OperationLog(module = "ACTION", action = "OFFLINE", targetType = "ACTION")
    @PostMapping("/{actionId}/offline")
    public ApiResult<Void> offline(@PathVariable Long actionId) {
        adminActionService.offline(actionId);
        return ApiResult.ok();
    }
}
