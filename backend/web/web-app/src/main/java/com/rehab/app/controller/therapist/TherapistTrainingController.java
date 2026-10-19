package com.rehab.app.controller.therapist;

import com.rehab.app.service.TherapistTrainingService;
import com.rehab.app.vo.PatientProgressVO;
import com.rehab.app.vo.PatientTrendPointVO;
import com.rehab.app.vo.TherapistAnnotationVO;
import com.rehab.app.vo.TherapistErrorLogVO;
import com.rehab.app.vo.TherapistTrainingRecordDetailVO;
import com.rehab.app.vo.TherapistTrainingRecordVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.AnnotationCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 治疗师端训练数据接口（P7-B1/P7-B2/P7-B3，接口文档 5.3）。
 *
 * <p>记录详情/导出不含原始视频与逐帧关键点；批注只增不改原始评分；
 * 归属经 plan.therapist_id 或 ACTIVE 绑定校验（403/404）；导出走审计。</p>
 */
@Tag(name = "治疗师端-训练数据")
@RestController
@RequestMapping("/therapist")
@RequiredArgsConstructor
@Validated
public class TherapistTrainingController {

    private final TherapistTrainingService therapistTrainingService;

    // ==================== 训练记录 ====================

    @Operation(summary = "分页查询负责患者训练记录（含 INSUFFICIENT，供区分表现差/拍摄条件差）")
    @GetMapping("/training-records")
    public ApiResult<PageResult<TherapistTrainingRecordVO>> listRecords(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) Long actionVersionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String poseQuality) {
        return ApiResult.ok(therapistTrainingService.listRecords(
                page, size, patientId, planId, actionVersionId, from, to, poseQuality));
    }

    @Operation(summary = "单次训练详情（指标+分项分+算法版本快照，不含视频/逐帧关键点）")
    @GetMapping("/training-records/{recordId}")
    public ApiResult<TherapistTrainingRecordDetailVO> getRecordDetail(@PathVariable Long recordId) {
        return ApiResult.ok(therapistTrainingService.getRecordDetail(recordId));
    }

    @Operation(summary = "分页查询纠错日志（维度/阶段/严重度/偏差/帧号）")
    @GetMapping("/training-records/{recordId}/errors")
    public ApiResult<PageResult<TherapistErrorLogVO>> listErrors(
            @PathVariable Long recordId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return ApiResult.ok(therapistTrainingService.listErrors(recordId, page, size));
    }

    // ==================== 复核批注 ====================

    @Operation(summary = "分页查询复核批注")
    @GetMapping("/training-records/{recordId}/annotations")
    public ApiResult<PageResult<TherapistAnnotationVO>> listAnnotations(
            @PathVariable Long recordId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return ApiResult.ok(therapistTrainingService.listAnnotations(recordId, page, size));
    }

    @Operation(summary = "添加复核批注（只增，不修改原始评分）")
    @PostMapping("/training-records/{recordId}/annotations")
    @OperationLog(module = "TRAINING", action = "ADD_ANNOTATION", targetType = "TRAINING_RECORD", recordParam = false)
    public ApiResult<TherapistAnnotationVO> addAnnotation(
            @PathVariable Long recordId,
            @Valid @RequestBody AnnotationCreateRequest request) {
        return ApiResult.ok(therapistTrainingService.addAnnotation(recordId, request.getAnnotation()));
    }

    // ==================== 趋势与完成度 ====================

    @Operation(summary = "患者趋势（得分/达标率/ROM/保持率/姿态质量，排除 INSUFFICIENT）")
    @GetMapping("/patients/{patientId}/trends")
    public ApiResult<List<PatientTrendPointVO>> getPatientTrends(
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String granularity) {
        return ApiResult.ok(therapistTrainingService.getPatientTrends(patientId, from, to, granularity));
    }

    @Operation(summary = "患者三级完成度（日/计划/动作项）")
    @GetMapping("/patients/{patientId}/completion")
    public ApiResult<PatientProgressVO> getPatientCompletion(
            @PathVariable Long patientId,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResult.ok(therapistTrainingService.getPatientCompletion(patientId, planId, from, to));
    }

    // ==================== 导出 ====================

    @Operation(summary = "导出训练汇总 CSV（不含病史/原始视频/逐帧关键点）")
    @GetMapping("/training-records/export")
    @OperationLog(module = "TRAINING", action = "EXPORT_TRAINING_CSV", targetType = "TRAINING_RECORD", recordParam = false)
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] body = therapistTrainingService.exportCsv(patientId, from, to);
        String filename = "training-records-" + patientId
                + (from != null ? "-" + from : "") + (to != null ? "-" + to : "") + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
