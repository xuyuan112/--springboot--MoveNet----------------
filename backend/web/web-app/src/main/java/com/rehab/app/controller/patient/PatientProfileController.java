package com.rehab.app.controller.patient;

import com.rehab.app.service.PatientProfileService;
import com.rehab.app.vo.PatientProfileVO;
import com.rehab.app.vo.PatientTherapistVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.model.dto.PatientProfileUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者端个人资料接口：/patient/profile、/patient/therapist。
 */
@Tag(name = "患者端 - 个人资料")
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    @Operation(summary = "查询本人完整资料")
    @GetMapping("/profile")
    public ApiResult<PatientProfileVO> getProfile() {
        return ApiResult.ok(patientProfileService.getMyProfile());
    }

    @Operation(summary = "修改本人开放字段资料")
    @PutMapping("/profile")
    @OperationLog(module = "USER", action = "UPDATE", targetType = "PATIENT_PROFILE", recordParam = true)
    public ApiResult<PatientProfileVO> updateProfile(@Valid @RequestBody PatientProfileUpdateRequest request) {
        return ApiResult.ok(patientProfileService.updateMyProfile(request));
    }

    @Operation(summary = "查询当前有效负责治疗师（无绑定返回 data:null）")
    @GetMapping("/therapist")
    public ApiResult<PatientTherapistVO> getMyTherapist() {
        PatientTherapistVO vo = patientProfileService.getMyTherapist();
        return ApiResult.ok(vo);
    }
}
