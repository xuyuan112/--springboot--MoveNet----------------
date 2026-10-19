package com.rehab.admin.controller.content;

import com.rehab.admin.service.AdminContentService;
import com.rehab.admin.vo.AdminContentDetailVO;
import com.rehab.admin.vo.AdminContentVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ContentAuditRequest;
import com.rehab.model.dto.HealthContentSaveRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 公共健康内容：/admin/contents（仅 ADMIN）。
 * 审核：APPROVE 通过 / REJECT 驳回（理由必填，回退草稿）/ OFFLINE 下架。
 */
@Tag(name = "管理端-公共内容审核")
@RestController
@RequestMapping("/admin/contents")
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminContentService adminContentService;

    @Operation(summary = "分页查询内容（状态/类型/关键词）")
    @GetMapping
    public ApiResult<PageResult<AdminContentVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(adminContentService.pageContents(page, size, status, category, keyword));
    }

    @Operation(summary = "创建健康建议/动作演示内容草稿")
    @OperationLog(module = "CONTENT", action = "CREATE", targetType = "CONTENT", recordParam = true)
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody HealthContentSaveRequest request) {
        return ApiResult.ok(adminContentService.createContent(request));
    }

    @Operation(summary = "查询内容详情（正文、适用部位、审核信息、附件预签名地址）")
    @GetMapping("/{contentId}")
    public ApiResult<AdminContentDetailVO> detail(@PathVariable Long contentId) {
        return ApiResult.ok(adminContentService.detail(contentId));
    }

    @Operation(summary = "编辑草稿或被驳回内容（附件全量替换）")
    @OperationLog(module = "CONTENT", action = "UPDATE", targetType = "CONTENT", recordParam = true)
    @PutMapping("/{contentId}")
    public ApiResult<Void> update(@PathVariable Long contentId,
                                  @Valid @RequestBody HealthContentSaveRequest request) {
        adminContentService.updateContent(contentId, request);
        return ApiResult.ok();
    }

    @Operation(summary = "内容审核：APPROVE / REJECT（理由必填）/ OFFLINE")
    @OperationLog(module = "CONTENT", action = "AUDIT", targetType = "CONTENT", recordParam = true)
    @PostMapping("/{contentId}/audit")
    public ApiResult<Void> audit(@PathVariable Long contentId,
                                 @Valid @RequestBody ContentAuditRequest request) {
        adminContentService.audit(contentId, request);
        return ApiResult.ok();
    }

    @Operation(summary = "删除未发布草稿（已发布内容请先下架）")
    @OperationLog(module = "CONTENT", action = "DELETE", targetType = "CONTENT")
    @DeleteMapping("/{contentId}")
    public ApiResult<Void> delete(@PathVariable Long contentId) {
        adminContentService.deleteContent(contentId);
        return ApiResult.ok();
    }
}
