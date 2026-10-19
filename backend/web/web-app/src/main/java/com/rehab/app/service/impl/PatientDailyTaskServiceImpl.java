package com.rehab.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.PatientDailyTaskService;
import com.rehab.app.vo.ActionRuleVO;
import com.rehab.app.vo.ActionTemplateVO;
import com.rehab.app.vo.PatientDailyTaskVO;
import com.rehab.app.vo.TaskActionSnapshotVO;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.ActionRule;
import com.rehab.model.entity.ActionTemplate;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.entity.PlanDailyTask;
import com.rehab.model.entity.PlanItem;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.TrainingRecord;
import com.rehab.model.enums.DailyTaskStatus;
import com.rehab.model.enums.PoseQuality;
import com.rehab.model.enums.TemplateStatus;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.action.ActionRuleMapper;
import com.rehab.model.mapper.action.ActionTemplateMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.model.mapper.plan.PlanDailyTaskMapper;
import com.rehab.model.mapper.plan.PlanItemMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.training.TrainingRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 患者端每日训练任务服务实现。
 *
 * <p>状态口径（需求 3.1.2）：{@code correct_reps >= target_reps} → COMPLETED，
 * {@code 0 < correct_reps < target_reps} → PARTIAL，无记录且过当天 23:59:59 由定时任务置 EXPIRED。
 * PARTIAL 不落库（枚举无此值），由查询时依据训练记录派生；患者不可直接修改状态与目标参数。</p>
 */
@Service
@RequiredArgsConstructor
public class PatientDailyTaskServiceImpl implements PatientDailyTaskService {

    /** 派生展示状态：部分完成（不落库） */
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_PARTIAL_NAME = "部分完成";

    private final PlanDailyTaskMapper planDailyTaskMapper;
    private final PlanItemMapper planItemMapper;
    private final RehabPlanMapper rehabPlanMapper;
    private final ActionVersionMapper actionVersionMapper;
    private final ActionMapper actionMapper;
    private final ActionRuleMapper actionRuleMapper;
    private final ActionTemplateMapper actionTemplateMapper;
    private final TrainingRecordMapper trainingRecordMapper;

    @Override
    public List<PatientDailyTaskVO> listTodayTasks() {
        Long userId = SecurityUtils.getUserId();
        List<Long> itemIds = listMyPlanItemIds(userId);
        if (itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlanDailyTask> tasks = planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                .in(PlanDailyTask::getPlanItemId, itemIds)
                .eq(PlanDailyTask::getTaskDate, LocalDate.now())
                .orderByAsc(PlanDailyTask::getTaskId));
        return toVOList(tasks);
    }

    @Override
    public PageResult<PatientDailyTaskVO> listMyTasks(Integer page, Integer size, LocalDate from, LocalDate to, String status) {
        Long userId = SecurityUtils.getUserId();
        if (StringUtils.hasText(status)) {
            validateTaskStatus(status);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        List<Long> itemIds = listMyPlanItemIds(userId);
        if (itemIds.isEmpty()) {
            return PageResult.empty(page.longValue(), size.longValue());
        }
        // PARTIAL 为派生状态，落库值为 COMPLETED：按 PARTIAL 过滤时改查 COMPLETED 再内存过滤
        String storedStatus = STATUS_PARTIAL.equals(status) ? DailyTaskStatus.COMPLETED.getCode() : status;
        Page<PlanDailyTask> mpPage = new Page<>(page, size);
        planDailyTaskMapper.selectPage(mpPage, new LambdaQueryWrapper<PlanDailyTask>()
                .in(PlanDailyTask::getPlanItemId, itemIds)
                .ge(from != null, PlanDailyTask::getTaskDate, from)
                .le(to != null, PlanDailyTask::getTaskDate, to)
                .eq(StringUtils.hasText(storedStatus), PlanDailyTask::getStatus, storedStatus)
                .orderByDesc(PlanDailyTask::getTaskDate)
                .orderByAsc(PlanDailyTask::getTaskId));
        List<PatientDailyTaskVO> voList = toVOList(mpPage.getRecords());
        if (STATUS_PARTIAL.equals(status)) {
            voList = voList.stream().filter(vo -> STATUS_PARTIAL.equals(vo.getStatus())).collect(Collectors.toList());
        }
        return PageResult.of(mpPage, voList);
    }

    @Override
    public PatientDailyTaskVO getMyTaskDetail(Long taskId) {
        PlanDailyTask task = requireOwnTask(taskId);
        return toVOList(List.of(task)).get(0);
    }

    @Override
    public TaskActionSnapshotVO startTask(Long taskId, String clientPoseEngineId) {
        PlanDailyTask task = requireOwnTask(taskId);
        // 过期任务（含定时任务尚未跑但日期已过的）一律 409
        if (DailyTaskStatus.EXPIRED.getCode().equals(task.getStatus())
                || task.getTaskDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "任务已过期，无法开始训练");
        }
        if (DailyTaskStatus.SKIPPED.getCode().equals(task.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "任务已跳过，无法开始训练");
        }
        if (DailyTaskStatus.COMPLETED.getCode().equals(task.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "任务已完成，无需重复开始");
        }

        // P3-F10 服务端兜底：端侧经 X-Pose-Engine-Id 上报其本地引擎标识，
        // 与该任务动作版本固化的 pose_engine_id 不一致则拒绝开始（状态机未被推进）。
        PlanItem guardItem = planItemMapper.selectById(task.getPlanItemId());
        if (guardItem == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务对应的计划项不存在");
        }
        ActionVersion guardVersion = actionVersionMapper.selectById(guardItem.getActionVersionId());
        if (guardVersion == null) {
            throw new BusinessException(ResultCode.VERSION_NOT_FOUND);
        }
        String serverEngineId = guardVersion.getPoseEngineId();
        String expectedEngineId = StringUtils.hasText(serverEngineId) ? serverEngineId.trim() : null;
        if (StringUtils.hasText(clientPoseEngineId)
                && StringUtils.hasText(expectedEngineId)
                && !clientPoseEngineId.trim().equals(expectedEngineId)) {
            throw new BusinessException(ResultCode.ENGINE_VERSION_MISMATCH,
                    "姿态引擎版本不一致：端侧 " + clientPoseEngineId.trim() + "，动作版本要求 " + expectedEngineId);
        }

        // 幂等：仅首次开始写入 IN_PROGRESS 与 startedAt，重复开始保留原开始时间
        if (DailyTaskStatus.PENDING.getCode().equals(task.getStatus())) {
            task.setStatus(DailyTaskStatus.IN_PROGRESS.getCode());
            task.setStartedAt(LocalDateTime.now());
            planDailyTaskMapper.updateById(task);
        }
        return buildSnapshot(task);
    }

    @Override
    public void pauseTask(Long taskId, String reason) {
        PlanDailyTask task = requireOwnTask(taskId);
        if (!DailyTaskStatus.IN_PROGRESS.getCode().equals(task.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅进行中的任务可以暂停");
        }
        // 暂停不改变状态（保持 IN_PROGRESS），原因由 @OperationLog 记录；重复暂停视为幂等
    }

    @Override
    public void finishTask(Long taskId, String reason) {
        PlanDailyTask task = requireOwnTask(taskId);
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "结束/跳过任务必须填写原因");
        }
        // 幂等：已跳过/已完成直接返回；已过期不可再结束
        if (DailyTaskStatus.SKIPPED.getCode().equals(task.getStatus())
                || DailyTaskStatus.COMPLETED.getCode().equals(task.getStatus())) {
            return;
        }
        if (DailyTaskStatus.EXPIRED.getCode().equals(task.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "任务已过期，无法结束");
        }
        int correctReps = sumCorrectReps(taskId);
        LocalDateTime now = LocalDateTime.now();
        if (correctReps > 0) {
            // 有训练记录：结束置 COMPLETED（达标与否由查询时按 correct/target 派生 COMPLETED/PARTIAL）
            task.setStatus(DailyTaskStatus.COMPLETED.getCode());
            task.setCompletedAt(now);
        } else {
            // 无训练记录：视为跳过，必须记录原因
            task.setStatus(DailyTaskStatus.SKIPPED.getCode());
            task.setSkipReason(reason.trim());
            task.setCompletedAt(now);
        }
        planDailyTaskMapper.updateById(task);
    }

    // ==================== 私有辅助 ====================

    /** 校验任务归属：任务→计划项→计划，计划患者必须等于当前用户，否则统一 404。 */
    private PlanDailyTask requireOwnTask(Long taskId) {
        PlanDailyTask task = planDailyTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        PlanItem item = planItemMapper.selectById(task.getPlanItemId());
        if (item == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        RehabPlan plan = rehabPlanMapper.selectById(item.getPlanId());
        if (plan == null || !plan.getPatientId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        return task;
    }

    /** 当前患者全部计划的全部动作项 ID。 */
    private List<Long> listMyPlanItemIds(Long userId) {
        List<RehabPlan> plans = rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getPatientId, userId)
                .select(RehabPlan::getPlanId));
        if (plans.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> planIds = plans.stream().map(RehabPlan::getPlanId).collect(Collectors.toList());
        List<PlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .in(PlanItem::getPlanId, planIds)
                .select(PlanItem::getPlanItemId));
        return items.stream().map(PlanItem::getPlanItemId).collect(Collectors.toList());
    }

    /** 该任务有效训练记录的达标次数合计（P4-B2：排除 INSUFFICIENT；已撤回由逻辑删除自动排除）。 */
    private int sumCorrectReps(Long taskId) {
        List<TrainingRecord> records = trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                .eq(TrainingRecord::getDailyTaskId, taskId)
                .ne(TrainingRecord::getPoseQuality, PoseQuality.INSUFFICIENT.getCode())
                .select(TrainingRecord::getCorrectReps));
        return records.stream()
                .map(TrainingRecord::getCorrectReps)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private void validateTaskStatus(String status) {
        if (STATUS_PARTIAL.equals(status)) {
            return;
        }
        try {
            DailyTaskStatus.fromCode(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.INVALID_ENUM, "非法的任务状态：" + status);
        }
    }

    /**
     * 批量组装 VO：补齐计划名/动作名，派生展示状态与进度。
     */
    private List<PatientDailyTaskVO> toVOList(List<PlanDailyTask> tasks) {
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> itemIds = tasks.stream().map(PlanDailyTask::getPlanItemId).distinct().collect(Collectors.toList());
        Map<Long, PlanItem> itemMap = planItemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(PlanItem::getPlanItemId, i -> i));
        List<Long> planIds = itemMap.values().stream().map(PlanItem::getPlanId).distinct().collect(Collectors.toList());
        Map<Long, RehabPlan> planMap = planIds.isEmpty() ? Collections.emptyMap()
                : rehabPlanMapper.selectBatchIds(planIds).stream()
                        .collect(Collectors.toMap(RehabPlan::getPlanId, p -> p));
        List<Long> versionIds = itemMap.values().stream().map(PlanItem::getActionVersionId).distinct().collect(Collectors.toList());
        Map<Long, ActionVersion> versionMap = versionIds.isEmpty() ? Collections.emptyMap()
                : actionVersionMapper.selectBatchIds(versionIds).stream()
                        .collect(Collectors.toMap(ActionVersion::getActionVersionId, v -> v));
        List<Long> actionIds = versionMap.values().stream().map(ActionVersion::getActionId).distinct().collect(Collectors.toList());
        Map<Long, Action> actionMap = actionIds.isEmpty() ? Collections.emptyMap()
                : actionMapper.selectBatchIds(actionIds).stream()
                        .collect(Collectors.toMap(Action::getActionId, a -> a));
        // 达标次数按任务聚合（P4-B2：排除 INSUFFICIENT，已撤回由逻辑删除自动排除）
        List<Long> taskIds = tasks.stream().map(PlanDailyTask::getTaskId).collect(Collectors.toList());
        Map<Long, Integer> correctRepsMap = trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                        .in(TrainingRecord::getDailyTaskId, taskIds)
                        .ne(TrainingRecord::getPoseQuality, PoseQuality.INSUFFICIENT.getCode())
                        .select(TrainingRecord::getDailyTaskId, TrainingRecord::getCorrectReps))
                .stream()
                .filter(r -> r.getCorrectReps() != null)
                .collect(Collectors.groupingBy(TrainingRecord::getDailyTaskId,
                        Collectors.summingInt(TrainingRecord::getCorrectReps)));

        return tasks.stream().map(task -> {
            PatientDailyTaskVO vo = new PatientDailyTaskVO();
            vo.setTaskId(task.getTaskId());
            vo.setPlanItemId(task.getPlanItemId());
            vo.setTaskDate(task.getTaskDate());
            vo.setTargetSets(task.getTargetSets());
            vo.setTargetReps(task.getTargetReps());
            vo.setSkipReason(task.getSkipReason());
            vo.setStartedAt(task.getStartedAt());
            vo.setCompletedAt(task.getCompletedAt());

            PlanItem item = itemMap.get(task.getPlanItemId());
            if (item != null) {
                vo.setPlanId(item.getPlanId());
                RehabPlan plan = planMap.get(item.getPlanId());
                vo.setPlanName(plan != null ? plan.getName() : null);
                ActionVersion version = versionMap.get(item.getActionVersionId());
                if (version != null) {
                    Action action = actionMap.get(version.getActionId());
                    vo.setActionName(action != null ? action.getName() : null);
                }
            }

            int correctReps = correctRepsMap.getOrDefault(task.getTaskId(), 0);
            vo.setCorrectReps(correctReps);
            int targetReps = task.getTargetReps() != null && task.getTargetReps() > 0 ? task.getTargetReps() : 0;
            vo.setProgressPercent(targetReps > 0
                    ? BigDecimal.valueOf(Math.min(100.0, correctReps * 100.0 / targetReps)).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            // 派生展示状态
            String stored = task.getStatus();
            if (DailyTaskStatus.EXPIRED.getCode().equals(stored) || DailyTaskStatus.SKIPPED.getCode().equals(stored)) {
                vo.setStatus(stored);
            } else if (targetReps > 0 && correctReps >= targetReps) {
                vo.setStatus(DailyTaskStatus.COMPLETED.getCode());
            } else if (correctReps > 0) {
                vo.setStatus(STATUS_PARTIAL);
            } else {
                vo.setStatus(stored);
            }
            vo.setStatusName(STATUS_PARTIAL.equals(vo.getStatus())
                    ? STATUS_PARTIAL_NAME
                    : DailyTaskStatus.fromCode(vo.getStatus()).getName());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 按 plan_daily_task → plan_item → action_version → action_rule → action_template 固定顺序读取参数快照。
     */
    private TaskActionSnapshotVO buildSnapshot(PlanDailyTask task) {
        PlanItem item = planItemMapper.selectById(task.getPlanItemId());
        if (item == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务对应的计划项不存在");
        }
        ActionVersion version = actionVersionMapper.selectById(item.getActionVersionId());
        if (version == null) {
            throw new BusinessException(ResultCode.VERSION_NOT_FOUND);
        }
        Action action = actionMapper.selectById(version.getActionId());
        List<ActionRule> rules = actionRuleMapper.selectList(new LambdaQueryWrapper<ActionRule>()
                .eq(ActionRule::getActionVersionId, version.getActionVersionId())
                .orderByAsc(ActionRule::getActionRuleId));
        ActionTemplate template = actionTemplateMapper.selectOne(new LambdaQueryWrapper<ActionTemplate>()
                .eq(ActionTemplate::getActionVersionId, version.getActionVersionId())
                .eq(ActionTemplate::getStatus, TemplateStatus.ACTIVE.getCode())
                .orderByDesc(ActionTemplate::getTemplateId)
                .last("limit 1"));

        TaskActionSnapshotVO vo = new TaskActionSnapshotVO();
        vo.setTaskId(task.getTaskId());
        vo.setPlanItemId(item.getPlanItemId());
        vo.setActionVersionId(version.getActionVersionId());
        vo.setActionName(action != null ? action.getName() : null);
        vo.setTargetSets(task.getTargetSets());
        vo.setTargetReps(task.getTargetReps());
        vo.setTargetAmplitude(item.getTargetAmplitude());
        vo.setDurationSec(item.getDurationSec());
        vo.setRestSec(item.getRestSec());
        vo.setInstruction(item.getInstruction());
        vo.setAlgoConfig(version.getAlgoConfig());
        vo.setMotionSignature(version.getMotionSignature());
        vo.setPoseEngineId(version.getPoseEngineId());
        vo.setRules(rules.stream().map(this::toRuleVO).collect(Collectors.toList()));
        vo.setTemplate(template != null ? toTemplateVO(template) : null);
        return vo;
    }

    private ActionRuleVO toRuleVO(ActionRule rule) {
        ActionRuleVO vo = new ActionRuleVO();
        vo.setActionRuleId(rule.getActionRuleId());
        vo.setJointName(rule.getJointName());
        vo.setKeypointIndices(rule.getKeypointIndices());
        vo.setAngleFormula(rule.getAngleFormula());
        vo.setJointRole(rule.getJointRole());
        vo.setDimension(rule.getDimension());
        vo.setPhase(rule.getPhase());
        vo.setMinAngle(rule.getMinAngle());
        vo.setMaxAngle(rule.getMaxAngle());
        vo.setStandardAngle(rule.getStandardAngle());
        vo.setToleranceSoft(rule.getToleranceSoft());
        vo.setToleranceHard(rule.getToleranceHard());
        vo.setHoldFramesRequired(rule.getHoldFramesRequired());
        vo.setSpeedMin(rule.getSpeedMin());
        vo.setSpeedMax(rule.getSpeedMax());
        vo.setScoreWeight(rule.getScoreWeight());
        vo.setSeverity(rule.getSeverity());
        vo.setPromptText(rule.getPromptText());
        vo.setSymmetryPairRuleId(rule.getSymmetryPairRuleId());
        return vo;
    }

    private ActionTemplateVO toTemplateVO(ActionTemplate template) {
        ActionTemplateVO vo = new ActionTemplateVO();
        vo.setTemplateId(template.getTemplateId());
        vo.setSourceType(template.getSourceType());
        vo.setFrameRate(template.getFrameRate());
        vo.setFrameCount(template.getFrameCount());
        vo.setFeatureDim(template.getFeatureDim());
        vo.setFeatureSequence(template.getFeatureSequence());
        vo.setKeyframeIndex(template.getKeyframeIndex());
        vo.setSampleCount(template.getSampleCount());
        vo.setStatus(template.getStatus());
        return vo;
    }
}
