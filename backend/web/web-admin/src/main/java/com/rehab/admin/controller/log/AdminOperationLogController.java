package com.rehab.admin.controller.log;

import com.rehab.admin.service.AdminOperationLogService;
import com.rehab.admin.vo.AdminOperationLogVO;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 管理端 - 操作审计日志：GET /admin/operation-logs（只读，仅 ADMIN）。
 * 查询接口本身不记录 operation_log。
 */
@Tag(name = "管理端-操作日志")
@RestController
@RequestMapping("/admin/operation-logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final AdminOperationLogService adminOperationLogService;

    @Operation(summary = "分页审计查询（模块/操作人/时间范围）")
    @GetMapping
    public ApiResult<PageResult<AdminOperationLogVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {
        return ApiResult.ok(adminOperationLogService.pageLogs(page, size, module, operatorId, from, to));
    }
}
