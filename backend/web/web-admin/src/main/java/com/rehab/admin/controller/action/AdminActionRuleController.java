package com.rehab.admin.controller.action;

import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.model.dto.ActionRuleRequest;
import com.rehab.admin.service.AdminActionRuleService;
import com.rehab.admin.vo.AdminActionRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 - 动作规则管理：/admin/action-versions/{versionId}/rules（仅 ADMIN）。
 */
@Tag(name = "管理端-动作规则管理")
@RestController
@RequestMapping("/admin/action-versions/{versionId}/rules")
@RequiredArgsConstructor
public class AdminActionRuleController {

    private final AdminActionRuleService adminActionRuleService;

    @Operation(summary = "查询指定版本下的规则列表")
    @GetMapping
    public ApiResult<List<AdminActionRuleVO>> list(@PathVariable Long versionId) {
        return ApiResult.ok(adminActionRuleService.listRules(versionId));
    }

    @Operation(summary = "创建规则")
    @OperationLog(module = "ACTION", action = "CREATE", targetType = "ACTION_RULE", recordParam = true)
    @PostMapping
    public ApiResult<Long> create(@PathVariable Long versionId,
                                  @Valid @RequestBody ActionRuleRequest request) {
        Long ruleId = adminActionRuleService.createRule(versionId, request);
        return ApiResult.ok(ruleId);
    }

    @Operation(summary = "修改规则")
    @OperationLog(module = "ACTION", action = "UPDATE", targetType = "ACTION_RULE", recordParam = true)
    @PutMapping("/{ruleId}")
    public ApiResult<Void> update(@PathVariable Long versionId,
                                  @PathVariable Long ruleId,
                                  @Valid @RequestBody ActionRuleRequest request) {
        adminActionRuleService.updateRule(versionId, ruleId, request);
        return ApiResult.ok();
    }

    @Operation(summary = "删除规则")
    @OperationLog(module = "ACTION", action = "DELETE", targetType = "ACTION_RULE")
    @DeleteMapping("/{ruleId}")
    public ApiResult<Void> delete(@PathVariable Long versionId,
                                  @PathVariable Long ruleId) {
        adminActionRuleService.deleteRule(versionId, ruleId);
        return ApiResult.ok();
    }
}
