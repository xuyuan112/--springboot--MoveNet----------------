package com.rehab.app.controller.patient;

import com.rehab.app.service.PatientCollectionService;
import com.rehab.app.service.PatientContentService;
import com.rehab.app.vo.PatientContentDetailVO;
import com.rehab.app.vo.PatientContentVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者端 - 公共内容只读接口：/patient/contents（仅 PATIENT，由 SecurityConfig 统一拦截）。
 * 患者端不提供任何上传/写接口，附件仅通过短时预签名 URL 访问（接口文档 3.5）。
 * 收藏动作写审计（@OperationLog），重复收藏/取消均幂等。
 */
@Tag(name = "患者端-公共内容")
@RestController
@RequestMapping("/patient/contents")
@RequiredArgsConstructor
public class PatientContentController {

    private final PatientContentService patientContentService;
    private final PatientCollectionService patientCollectionService;

    @Operation(summary = "分页查询已发布且在有效期内的内容")
    @GetMapping
    public ApiResult<PageResult<PatientContentVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(patientContentService.pagePublished(page, size, partCode, category, keyword));
    }

    @Operation(summary = "查询内容正文、适用人群与附件预签名地址")
    @GetMapping("/{contentId}")
    public ApiResult<PatientContentDetailVO> detail(@PathVariable Long contentId) {
        return ApiResult.ok(patientContentService.getPublished(contentId));
    }

    @Operation(summary = "收藏内容（重复请求幂等）")
    @PostMapping("/{contentId}/collect")
    @OperationLog(module = "CONTENT", action = "COLLECT", targetType = "HEALTH_CONTENT", recordParam = false)
    public ApiResult<Void> collect(@PathVariable Long contentId) {
        patientCollectionService.collect(contentId);
        return ApiResult.ok();
    }

    @Operation(summary = "取消收藏（未收藏时幂等成功）")
    @DeleteMapping("/{contentId}/collect")
    @OperationLog(module = "CONTENT", action = "UNCOLLECT", targetType = "HEALTH_CONTENT", recordParam = false)
    public ApiResult<Void> uncollect(@PathVariable Long contentId) {
        patientCollectionService.uncollect(contentId);
        return ApiResult.ok();
    }
}
