package com.rehab.app.controller.patient;

import com.rehab.app.service.PatientDailyTaskService;
import com.rehab.app.vo.PatientDailyTaskVO;
import com.rehab.app.vo.TaskActionSnapshotVO;
import com.rehab.common.annotation.OperationLog;
import com.rehab.common.result.ApiResult;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.ReasonRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 患者端每日训练任务接口：/patient/tasks/**。
 */
@Tag(name = "患者端 - 每日任务")
@Validated
@RestController
@RequestMapping("/patient/tasks")
@RequiredArgsConstructor
public class PatientTaskController {

    private final PatientDailyTaskService patientDailyTaskService;

    @Operation(summary = "查询今日任务聚合列表")
    @GetMapping("/today")
    public ApiResult<List<PatientDailyTaskVO>> listTodayTasks() {
        return ApiResult.ok(patientDailyTaskService.listTodayTasks());
    }

    @Operation(summary = "按日期、状态分页查询每日任务")
    @GetMapping
    public ApiResult<PageResult<PatientDailyTaskVO>> listTasks(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(patientDailyTaskService.listMyTasks(page, size, from, to, status));
    }

    @Operation(summary = "查询单条任务及完成进度")
    @GetMapping("/{taskId}")
    public ApiResult<PatientDailyTaskVO> getTaskDetail(@PathVariable Long taskId) {
        return ApiResult.ok(patientDailyTaskService.getMyTaskDetail(taskId));
    }

    @Operation(summary = "开始训练，返回动作版本参数快照")
    @PostMapping("/{taskId}/start")
    @OperationLog(module = "TRAINING", action = "START", targetType = "PLAN_DAILY_TASK", recordParam = false)
    public ApiResult<TaskActionSnapshotVO> startTask(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-Pose-Engine-Id", required = false) String clientPoseEngineId) {
        return ApiResult.ok(patientDailyTaskService.startTask(taskId, clientPoseEngineId));
    }

    @Operation(summary = "暂停训练并记录原因")
    @PostMapping("/{taskId}/pause")
    @OperationLog(module = "TRAINING", action = "PAUSE", targetType = "PLAN_DAILY_TASK", recordParam = true)
    public ApiResult<Void> pauseTask(@PathVariable Long taskId, @Valid @RequestBody ReasonRequest request) {
        patientDailyTaskService.pauseTask(taskId, request.getReason());
        return ApiResult.ok();
    }

    @Operation(summary = "主动结束/跳过任务，必须提交原因")
    @PostMapping("/{taskId}/finish")
    @OperationLog(module = "TRAINING", action = "FINISH", targetType = "PLAN_DAILY_TASK", recordParam = true)
    public ApiResult<Void> finishTask(@PathVariable Long taskId, @Valid @RequestBody ReasonRequest request) {
        patientDailyTaskService.finishTask(taskId, request.getReason());
        return ApiResult.ok();
    }
}
