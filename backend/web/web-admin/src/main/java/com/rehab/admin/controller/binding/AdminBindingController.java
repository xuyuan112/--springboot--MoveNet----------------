package com.rehab.admin.controller.binding;

import com.rehab.admin.service.AdminBindingService;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.BindingCreateRequest;
import com.rehab.model.dto.BindingUpdateRequest;
import com.rehab.model.dto.ReasonRequest;
import com.rehab.admin.vo.BindingVO;
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
 * 管理端 - 医患绑定关系管理（/admin/bindings，仅 ADMIN）。
 * 建立、变更、解除患者与治疗师的负责关系；解绑仅状态流转，历史数据只读保留。
 */
@Tag(name = "管理端-绑定关系管理")
@RestController
@RequestMapping("/admin/bindings")
@RequiredArgsConstructor
public class AdminBindingController {

    private final AdminBindingService adminBindingService;

    @Operation(summary = "分页查询绑定关系（患者/治疗师/状态）")
    @GetMapping
    public ApiResult<PageResult<BindingVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long therapistId,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(adminBindingService.pageBindings(page, size, patientId, therapistId, status));
    }

    @Operation(summary = "建立主责/协作绑定（校验双方启用及主责冲突）")
    @OperationLog(module = "BINDING", action = "CREATE", targetType = "PATIENT_THERAPIST", recordParam = true)
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody BindingCreateRequest request) {
        return ApiResult.ok(adminBindingService.createBinding(request));
    }

    @Operation(summary = "变更关系类型或生效期（仅有效绑定）")
    @OperationLog(module = "BINDING", action = "UPDATE", targetType = "PATIENT_THERAPIST", recordParam = true)
    @PutMapping("/{bindingId}")
    public ApiResult<Void> update(@PathVariable Long bindingId,
                                  @Valid @RequestBody BindingUpdateRequest request) {
        adminBindingService.updateBinding(bindingId, request);
        return ApiResult.ok();
    }

    @Operation(summary = "解除绑定（历史数据保留只读，需填写原因）")
    @OperationLog(module = "BINDING", action = "UNBIND", targetType = "PATIENT_THERAPIST", recordParam = true)
    @PostMapping("/{bindingId}/unbind")
    public ApiResult<Void> unbind(@PathVariable Long bindingId,
                                  @Valid @RequestBody ReasonRequest reason) {
        adminBindingService.unbind(bindingId, reason);
        return ApiResult.ok();
    }
}
