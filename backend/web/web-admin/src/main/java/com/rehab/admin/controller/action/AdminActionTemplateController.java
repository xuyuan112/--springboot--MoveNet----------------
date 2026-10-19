package com.rehab.admin.controller.action;

import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ActionTemplateSaveRequest;
import com.rehab.admin.service.AdminActionTemplateService;
import com.rehab.admin.vo.AdminActionTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 动作模板管理：/admin/action-versions/{versionId}/templates（仅 ADMIN）。
 */
@Tag(name = "管理端-动作模板管理")
@RestController
@RequiredArgsConstructor
public class AdminActionTemplateController {

    private final AdminActionTemplateService adminActionTemplateService;

    @Operation(summary = "分页查询指定版本下的模板")
    @GetMapping("/admin/action-versions/{versionId}/templates")
    public ApiResult<PageResult<AdminActionTemplateVO>> page(
            @PathVariable Long versionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResult.ok(adminActionTemplateService.pageTemplates(versionId, page, size));
    }

    @Operation(summary = "创建模板（上传示范数据生成归一化特征）")
    @OperationLog(module = "ACTION", action = "CREATE", targetType = "ACTION_TEMPLATE", recordParam = true)
    @PostMapping("/admin/action-versions/{versionId}/templates")
    public ApiResult<Long> create(@PathVariable Long versionId,
                                  @Valid @RequestBody ActionTemplateSaveRequest request) {
        Long templateId = adminActionTemplateService.createTemplate(versionId, request);
        return ApiResult.ok(templateId);
    }

    @Operation(summary = "发布模板")
    @OperationLog(module = "ACTION", action = "PUBLISH", targetType = "ACTION_TEMPLATE")
    @PostMapping("/admin/templates/{templateId}/publish")
    public ApiResult<Void> publish(@PathVariable Long templateId) {
        adminActionTemplateService.publish(templateId);
        return ApiResult.ok();
    }

    @Operation(summary = "删除模板（未被引用时）")
    @OperationLog(module = "ACTION", action = "DELETE", targetType = "ACTION_TEMPLATE")
    @DeleteMapping("/admin/templates/{templateId}")
    public ApiResult<Void> delete(@PathVariable Long templateId) {
        adminActionTemplateService.deleteTemplate(templateId);
        return ApiResult.ok();
    }
}
