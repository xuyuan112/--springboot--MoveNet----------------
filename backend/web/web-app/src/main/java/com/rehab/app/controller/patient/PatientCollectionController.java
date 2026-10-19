package com.rehab.app.controller.patient;

import com.rehab.app.service.PatientCollectionService;
import com.rehab.app.vo.PatientCollectionVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者端收藏列表接口（P5-B3）：/patient/collections。
 *
 * <p>收藏/取消收藏动作挂在 /patient/contents/{contentId}/collect（见 PatientContentController）。</p>
 */
@Tag(name = "患者端 - 内容收藏")
@Validated
@RestController
@RequestMapping("/patient/collections")
@RequiredArgsConstructor
public class PatientCollectionController {

    private final PatientCollectionService patientCollectionService;

    @Operation(summary = "分页查询本人收藏（下架内容仅保留标题占位）")
    @GetMapping
    public ApiResult<PageResult<PatientCollectionVO>> pageMyCollections(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        return ApiResult.ok(patientCollectionService.pageMyCollections(page, size));
    }
}
