package com.rehab.admin.controller.action;

import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ActionVersionSaveRequest;
import com.rehab.admin.service.AdminActionVersionService;
import com.rehab.admin.vo.AdminActionVersionDetailVO;
import com.rehab.admin.vo.AdminActionVersionVO;
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
 * 管理端 - 动作版本管理（仅 ADMIN）。
 *
 * <p>创建版本端点遵循接口文档挂在 /admin/actions/{actionId}/versions，
 * 其余端点挂在 /admin/action-versions，故类级不设置 RequestMapping。</p>
 */
@Tag(name = "管理端-动作版本管理")
@RestController
@RequiredArgsConstructor
public class AdminActionVersionController {

    private final AdminActionVersionService adminActionVersionService;

    @Operation(summary = "分页查询动作版本")
    @GetMapping("/admin/action-versions")
    public ApiResult<PageResult<AdminActionVersionVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long actionId,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(adminActionVersionService.pageVersions(page, size, actionId, status));
    }

    @Operation(summary = "创建动作版本（versionNo 自动递增）")
    @OperationLog(module = "ACTION", action = "CREATE", targetType = "ACTION_VERSION", recordParam = true)
    @PostMapping("/admin/actions/{actionId}/versions")
    public ApiResult<Long> create(@PathVariable Long actionId,
                                  @Valid @RequestBody ActionVersionSaveRequest request) {
        Long versionId = adminActionVersionService.createVersion(actionId, request);
        return ApiResult.ok(versionId);
    }

    @Operation(summary = "查询版本详情（含规则与模板列表）")
    @GetMapping("/admin/action-versions/{versionId}")
    public ApiResult<AdminActionVersionDetailVO> detail(@PathVariable Long versionId) {
        return ApiResult.ok(adminActionVersionService.detail(versionId));
    }

    @Operation(summary = "修改版本（仅草稿可编辑）")
    @OperationLog(module = "ACTION", action = "UPDATE", targetType = "ACTION_VERSION", recordParam = true)
    @PutMapping("/admin/action-versions/{versionId}")
    public ApiResult<Void> update(@PathVariable Long versionId,
                                  @Valid @RequestBody ActionVersionSaveRequest request) {
        adminActionVersionService.updateVersion(versionId, request);
        return ApiResult.ok();
    }

    @Operation(summary = "发布版本（校验权重和=1）")
    @OperationLog(module = "ACTION", action = "PUBLISH", targetType = "ACTION_VERSION")
    @PostMapping("/admin/action-versions/{versionId}/publish")
    public ApiResult<Void> publish(@PathVariable Long versionId) {
        adminActionVersionService.publish(versionId);
        return ApiResult.ok();
    }
}
