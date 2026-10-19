package com.rehab.admin.controller.backup;

import com.rehab.admin.service.AdminBackupService;
import com.rehab.admin.vo.BackupTaskVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.BackupRestoreRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 数据库备份与恢复：/admin/backups（仅 ADMIN）。
 */
@Tag(name = "管理端-备份恢复")
@RestController
@RequestMapping("/admin/backups")
@RequiredArgsConstructor
public class AdminBackupController {

    private final AdminBackupService adminBackupService;

    @Operation(summary = "创建数据库备份（异步执行，返回任务ID）")
    @OperationLog(module = "BACKUP", action = "CREATE", targetType = "DB_BACKUP")
    @PostMapping
    public ApiResult<Long> create() {
        return ApiResult.ok(adminBackupService.createBackup());
    }

    @Operation(summary = "分页查询备份任务")
    @GetMapping
    public ApiResult<PageResult<BackupTaskVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType) {
        return ApiResult.ok(adminBackupService.pageBackups(page, size, status, taskType));
    }

    @Operation(summary = "备份任务详情（成功备份一次性下发恢复确认码，5 分钟有效）")
    @GetMapping("/{backupId}")
    public ApiResult<BackupTaskVO> detail(@PathVariable Long backupId) {
        return ApiResult.ok(adminBackupService.getBackupDetail(backupId));
    }

    @Operation(summary = "二次确认后恢复备份（必须携带确认码与原因，恢复前自动生成安全快照）")
    @PostMapping("/{backupId}/restore")
    public ApiResult<Void> restore(@PathVariable Long backupId,
                                   @Valid @RequestBody BackupRestoreRequest request) {
        adminBackupService.restore(backupId, request);
        return ApiResult.ok();
    }
}
