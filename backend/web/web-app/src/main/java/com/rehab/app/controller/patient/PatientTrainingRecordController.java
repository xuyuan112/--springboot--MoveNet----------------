package com.rehab.app.controller.patient;

import com.rehab.app.service.TrainingRecordService;
import com.rehab.app.vo.TrainingRecordDetailVO;
import com.rehab.app.vo.TrainingRecordSubmitVO;
import com.rehab.app.vo.TrainingRecordVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ReasonRequest;
import com.rehab.model.dto.TrainingRecordSubmitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 患者端训练记录接口（P4-B2/P4-B3）：/patient/training-records/**。
 *
 * <p>提交/撤回写审计（@OperationLog）；列表与详情不返回原始视频与逐帧关键点；
 * 患者ID一律由令牌确定，忽略客户端传入。</p>
 */
@Tag(name = "患者端 - 训练记录")
@Validated
@RestController
@RequestMapping("/patient/training-records")
@RequiredArgsConstructor
public class PatientTrainingRecordController {

    private final TrainingRecordService trainingRecordService;

    @Operation(summary = "提交训练汇总（clientRequestId 幂等）")
    @PostMapping
    @OperationLog(module = "TRAINING", action = "SUBMIT_RECORD", targetType = "TRAINING_RECORD", recordParam = false)
    public ApiResult<TrainingRecordSubmitVO> submitTrainingRecord(
            @Valid @RequestBody TrainingRecordSubmitRequest request) {
        return ApiResult.ok(trainingRecordService.submitTrainingRecord(request));
    }

    @Operation(summary = "分页查询本人训练记录（日期/计划/动作筛选）")
    @GetMapping
    public ApiResult<PageResult<TrainingRecordVO>> listMyRecords(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) Long actionVersionId) {
        return ApiResult.ok(trainingRecordService.listMyRecords(page, size, from, to, planId, actionVersionId));
    }

    @Operation(summary = "查询单次训练详情（分项评分/关键指标/纠错摘要）")
    @GetMapping("/{recordId}")
    public ApiResult<TrainingRecordDetailVO> getMyRecordDetail(@PathVariable Long recordId) {
        return ApiResult.ok(trainingRecordService.getMyRecordDetail(recordId));
    }

    @Operation(summary = "撤回训练记录（软删除，原因必填；已纳入报告不可撤回）")
    @DeleteMapping("/{recordId}")
    @OperationLog(module = "TRAINING", action = "WITHDRAW_RECORD", targetType = "TRAINING_RECORD", recordParam = true)
    public ApiResult<Void> withdrawRecord(@PathVariable Long recordId,
                                          @Valid @RequestBody ReasonRequest request) {
        trainingRecordService.withdrawRecord(recordId, request.getReason());
        return ApiResult.ok();
    }
}
