package com.rehab.admin.controller.file;

import com.rehab.admin.service.AdminFileService;
import com.rehab.admin.vo.FilePresignVO;
import com.rehab.admin.vo.FileUrlVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.model.dto.FileCompleteRequest;
import com.rehab.model.dto.FilePresignRequest;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 文件直传与预览：/admin/files（仅 ADMIN）。
 * 前端申请预签名地址后直传 MinIO，complete 时完成大小/MIME/SHA-256 校验。
 */
@Tag(name = "管理端-文件管理")
@RestController
@RequestMapping("/admin/files")
@RequiredArgsConstructor
public class AdminFileController {

    private final AdminFileService adminFileService;

    @Operation(summary = "申请预签名直传地址（视频/图片/PDF 白名单）")
    @OperationLog(module = "FILE", action = "PRESIGN", targetType = "FILE", recordParam = true)
    @PostMapping("/presign")
    public ApiResult<FilePresignVO> presign(@Valid @RequestBody FilePresignRequest request) {
        return ApiResult.ok(adminFileService.presign(request));
    }

    @Operation(summary = "直传完成确认（校验大小、类型、SHA-256）")
    @OperationLog(module = "FILE", action = "COMPLETE", targetType = "FILE", recordParam = true)
    @PostMapping("/{uploadId}/complete")
    public ApiResult<Long> complete(@PathVariable String uploadId,
                                    @Valid @RequestBody FileCompleteRequest request) {
        return ApiResult.ok(adminFileService.complete(uploadId, request));
    }

    @Operation(summary = "获取文件预签名预览地址")
    @GetMapping("/{fileId}/url")
    public ApiResult<FileUrlVO> url(@PathVariable Long fileId) {
        return ApiResult.ok(adminFileService.getUrl(fileId));
    }

    @Operation(summary = "删除未被内容/模板/消息引用的文件")
    @OperationLog(module = "FILE", action = "DELETE", targetType = "FILE")
    @DeleteMapping("/{fileId}")
    public ApiResult<Void> delete(@PathVariable Long fileId) {
        adminFileService.delete(fileId);
        return ApiResult.ok();
    }
}
