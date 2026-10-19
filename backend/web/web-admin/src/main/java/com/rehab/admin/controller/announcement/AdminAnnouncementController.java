package com.rehab.admin.controller.announcement;

import com.rehab.admin.service.AdminAnnouncementService;
import com.rehab.admin.vo.AdminAnnouncementVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.AnnouncementPublishRequest;
import com.rehab.model.dto.AnnouncementSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * 管理端 - 平台公告：/admin/announcements（仅 ADMIN）。
 */
@Tag(name = "管理端-公告管理")
@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AdminAnnouncementService adminAnnouncementService;

    @Operation(summary = "分页查询公告")
    @GetMapping
    public ApiResult<PageResult<AdminAnnouncementVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(adminAnnouncementService.pageAnnouncements(page, size, status, scope, keyword));
    }

    @Operation(summary = "创建公告草稿")
    @OperationLog(module = "NOTICE", action = "CREATE", targetType = "ANNOUNCEMENT", recordParam = true)
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody AnnouncementSaveRequest request) {
        return ApiResult.ok(adminAnnouncementService.createAnnouncement(request));
    }

    @Operation(summary = "查询公告详情")
    @GetMapping("/{announcementId}")
    public ApiResult<AdminAnnouncementVO> detail(
            @Parameter(description = "公告ID") @PathVariable Long announcementId) {
        return ApiResult.ok(adminAnnouncementService.getAnnouncement(announcementId));
    }

    @Operation(summary = "修改公告（仅草稿/已下架可编辑）")
    @OperationLog(module = "NOTICE", action = "UPDATE", targetType = "ANNOUNCEMENT", recordParam = true)
    @PutMapping("/{announcementId}")
    public ApiResult<Void> update(@PathVariable Long announcementId,
                                  @Valid @RequestBody AnnouncementSaveRequest request) {
        adminAnnouncementService.updateAnnouncement(announcementId, request);
        return ApiResult.ok();
    }

    @Operation(summary = "发布公告并设置有效期")
    @OperationLog(module = "NOTICE", action = "PUBLISH", targetType = "ANNOUNCEMENT", recordParam = true)
    @PostMapping("/{announcementId}/publish")
    public ApiResult<Void> publish(@PathVariable Long announcementId,
                                   @Valid @RequestBody AnnouncementPublishRequest request) {
        adminAnnouncementService.publish(announcementId, request);
        return ApiResult.ok();
    }

    @Operation(summary = "下架公告")
    @OperationLog(module = "NOTICE", action = "OFFLINE", targetType = "ANNOUNCEMENT")
    @PostMapping("/{announcementId}/offline")
    public ApiResult<Void> offline(@PathVariable Long announcementId) {
        adminAnnouncementService.offline(announcementId);
        return ApiResult.ok();
    }

    @Operation(summary = "删除公告（仅草稿/已下架）")
    @OperationLog(module = "NOTICE", action = "DELETE", targetType = "ANNOUNCEMENT")
    @DeleteMapping("/{announcementId}")
    public ApiResult<Void> remove(@PathVariable Long announcementId) {
        adminAnnouncementService.deleteAnnouncement(announcementId);
        return ApiResult.ok();
    }
}
