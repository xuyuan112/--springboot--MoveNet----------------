package com.rehab.app.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.PatientProgressService;
import com.rehab.app.vo.PatientProgressPageVO;
import com.rehab.app.vo.PatientProgressVO;
import com.rehab.app.vo.PatientTaskProgressVO;
import com.rehab.app.vo.PatientTrendPointVO;
import com.rehab.common.constant.RedisKeyConstants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.entity.PlanDailyTask;
import com.rehab.model.entity.PlanItem;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.TrainingRecord;
import com.rehab.model.enums.DailyTaskStatus;
import com.rehab.model.enums.PoseQuality;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.model.mapper.plan.PlanDailyTaskMapper;
import com.rehab.model.mapper.plan.PlanItemMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.training.TrainingRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 患者进度/趋势服务实现（P5-B1）。
 *
 * <p>口径约定：</p>
 * <ul>
 *   <li>完成度统计排除 SKIPPED 任务（跳过不属于应执行范围）；</li>
 *   <li>达标次数/趋势排除 INSUFFICIENT 记录与已撤回记录（逻辑删除自动排除）；</li>
 *   <li>日级完成度走 Redis 今日缓存（写后失效，TTL 15 分钟兜底过期）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientProgressServiceImpl implements PatientProgressService {

    /** 今日聚合缓存 TTL：15 分钟（正常由训练提交/撤回主动失效） */
    private static final Duration TODAY_CACHE_TTL = Duration.ofMinutes(15);

    private final RehabPlanMapper rehabPlanMapper;
    private final PlanItemMapper planItemMapper;
    private final PlanDailyTaskMapper planDailyTaskMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final ActionVersionMapper actionVersionMapper;
    private final ActionMapper actionMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PatientProgressPageVO getProgress(Integer page, Integer size, LocalDate from, LocalDate to,
                                             Long planId) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        Long userId = SecurityUtils.getUserId();
        Scope scope = loadScope(userId, planId, from, to);

        PatientProgressPageVO result = new PatientProgressPageVO();
        result.setSummary(buildSummary(userId, scope, from, to));

        Page<PlanDailyTask> mpPage = new Page<>(page, size);
        if (scope.taskQuery() != null) {
            planDailyTaskMapper.selectPage(mpPage, scope.taskQuery()
                    .orderByDesc(PlanDailyTask::getTaskDate)
                    .orderByDesc(PlanDailyTask::getTaskId));
        }
        result.setRecords(PageResult.of(mpPage, toProgressRows(scope, mpPage.getRecords())));
        return result;
    }

    @Override
    public List<PatientTrendPointVO> getTrends(LocalDate from, LocalDate to, Long actionVersionId,
                                               String granularity) {
        return doGetTrends(SecurityUtils.getUserId(), from, to, actionVersionId, granularity);
    }

    @Override
    public PatientProgressVO getCompletionForPatient(Long patientUserId, Long planId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        Scope scope = loadScope(patientUserId, planId, from, to);
        return buildSummary(patientUserId, scope, from, to);
    }

    @Override
    public List<PatientTrendPointVO> getTrendsForPatient(Long patientUserId, LocalDate from, LocalDate to,
                                                         Long actionVersionId, String granularity) {
        return doGetTrends(patientUserId, from, to, actionVersionId, granularity);
    }

    /** 趋势聚合主体（患者端与治疗师端共用，仅用户维度不同）。 */
    private List<PatientTrendPointVO> doGetTrends(Long userId, LocalDate from, LocalDate to,
                                                  Long actionVersionId, String granularity) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        String gran = StringUtils.hasText(granularity) ? granularity.trim().toUpperCase() : "DAY";
        if (!"DAY".equals(gran) && !"WEEK".equals(gran)) {
            throw new BusinessException(ResultCode.INVALID_ENUM, "非法的统计粒度：" + granularity + "（仅支持 DAY/WEEK）");
        }
        List<Long> taskIds = listMyTaskIds(userId, null);
        if (taskIds.isEmpty()) {
            return List.of();
        }
        // 注意：MP 条件实参为急切求值，null 日期先归一为 null 时间（P4 教训）
        LocalDateTime fromTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toTime = to != null ? to.atTime(23, 59, 59) : null;
        List<TrainingRecord> records = trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                .in(TrainingRecord::getDailyTaskId, taskIds)
                .ne(TrainingRecord::getPoseQuality, PoseQuality.INSUFFICIENT.getCode())
                .eq(actionVersionId != null, TrainingRecord::getActionVersionId, actionVersionId)
                .ge(fromTime != null, TrainingRecord::getStartedAt, fromTime)
                .le(toTime != null, TrainingRecord::getStartedAt, toTime));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> targetRepsByTask = planDailyTaskMapper.selectBatchIds(
                        records.stream().map(TrainingRecord::getDailyTaskId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(PlanDailyTask::getTaskId,
                        t -> t.getTargetReps() != null ? t.getTargetReps() : 0));

        Map<String, List<TrainingRecord>> buckets = new TreeMap<>();
        for (TrainingRecord record : records) {
            LocalDate date = record.getStartedAt().toLocalDate();
            String key = "WEEK".equals(gran)
                    ? date.with(DayOfWeek.MONDAY).toString()
                    : date.toString();
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }
        return buckets.entrySet().stream().map(e -> toTrendPoint(e.getKey(), e.getValue(), targetRepsByTask))
                .toList();
    }

    // ==================== 私有辅助 ====================

    /** 查询范围：计划/计划项映射 + 任务条件构造器（无任务时 taskQuery 为 null）。 */
    private record Scope(Map<Long, RehabPlan> planMap,
                         Map<Long, PlanItem> itemMap,
                         Map<Long, String> actionNameByItem,
                         LambdaQueryWrapper<PlanDailyTask> taskQuery) {
    }

    private Scope loadScope(Long userId, Long planId, LocalDate from, LocalDate to) {
        List<RehabPlan> plans = rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getPatientId, userId)
                .eq(planId != null, RehabPlan::getPlanId, planId));
        if (plans.isEmpty()) {
            return new Scope(Map.of(), Map.of(), Map.of(), null);
        }
        Map<Long, RehabPlan> planMap = plans.stream()
                .collect(Collectors.toMap(RehabPlan::getPlanId, Function.identity()));
        List<PlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .in(PlanItem::getPlanId, planMap.keySet()));
        if (items.isEmpty()) {
            return new Scope(planMap, Map.of(), Map.of(), null);
        }
        Map<Long, PlanItem> itemMap = items.stream()
                .collect(Collectors.toMap(PlanItem::getPlanItemId, Function.identity()));

        // 动作名称批量解析：计划项 → 动作版本 → 动作
        Map<Long, String> actionNames = resolveActionNames(items);

        // 注意：LocalDate 直接传参（变量引用），不存在 P4 那种急切求值陷阱
        LambdaQueryWrapper<PlanDailyTask> taskQuery = new LambdaQueryWrapper<PlanDailyTask>()
                .in(PlanDailyTask::getPlanItemId, itemMap.keySet())
                .ge(from != null, PlanDailyTask::getTaskDate, from)
                .le(to != null, PlanDailyTask::getTaskDate, to);
        return new Scope(planMap, itemMap, actionNames, taskQuery);
    }

    private Map<Long, String> resolveActionNames(List<PlanItem> items) {
        List<Long> versionIds = items.stream()
                .map(PlanItem::getActionVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (versionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> actionIdByVersion = actionVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(ActionVersion::getActionVersionId, ActionVersion::getActionId));
        if (actionIdByVersion.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> nameByAction = actionMapper.selectBatchIds(actionIdByVersion.values()).stream()
                .collect(Collectors.toMap(Action::getActionId, Action::getName));
        Map<Long, String> result = new LinkedHashMap<>();
        actionIdByVersion.forEach((versionId, actionId) -> {
            String name = nameByAction.get(actionId);
            if (name != null) {
                result.put(versionId, name);
            }
        });
        return result;
    }

    /** 三级完成度聚合；日级部分走 Redis 今日缓存。 */
    private PatientProgressVO buildSummary(Long userId, Scope scope, LocalDate from, LocalDate to) {
        PatientProgressVO vo = new PatientProgressVO();
        vo.setDay(loadTodaySummaryCached(userId, scope));

        List<PlanDailyTask> filtered = scope.taskQuery() == null
                ? List.of()
                : planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                        .in(PlanDailyTask::getPlanItemId, scope.itemMap().keySet())
                        .ge(from != null, PlanDailyTask::getTaskDate, from)
                        .le(to != null, PlanDailyTask::getTaskDate, to));
        List<PlanDailyTask> countable = filtered.stream()
                .filter(t -> !DailyTaskStatus.SKIPPED.getCode().equals(t.getStatus()))
                .toList();
        long completed = countable.stream()
                .filter(t -> DailyTaskStatus.COMPLETED.getCode().equals(t.getStatus()))
                .count();

        PatientProgressVO.PlanLevel planLevel = new PatientProgressVO.PlanLevel();
        planLevel.setCompletedTasks((int) completed);
        planLevel.setTotalTasks(countable.size());
        planLevel.setPercent(percent((int) completed, countable.size()));
        vo.setPlan(planLevel);

        // 动作项级
        Map<Long, List<PlanDailyTask>> byItem = countable.stream()
                .collect(Collectors.groupingBy(PlanDailyTask::getPlanItemId));
        List<PatientProgressVO.ActionItemLevel> actionItems = new ArrayList<>();
        byItem.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    PlanItem item = scope.itemMap().get(e.getKey());
                    if (item == null) {
                        return;
                    }
                    long done = e.getValue().stream()
                            .filter(t -> DailyTaskStatus.COMPLETED.getCode().equals(t.getStatus()))
                            .count();
                    PatientProgressVO.ActionItemLevel level = new PatientProgressVO.ActionItemLevel();
                    level.setPlanItemId(e.getKey());
                    level.setActionName(scope.actionNameByItem().get(item.getActionVersionId()));
                    RehabPlan plan = scope.planMap().get(item.getPlanId());
                    level.setPlanName(plan != null ? plan.getName() : null);
                    level.setCompletedTasks((int) done);
                    level.setTotalTasks(e.getValue().size());
                    level.setPercent(percent((int) done, e.getValue().size()));
                    actionItems.add(level);
                });
        vo.setActionItems(actionItems);
        return vo;
    }

    /** 日级完成度（当日）：Redis 缓存优先，未命中回源计算。 */
    private PatientProgressVO.DayLevel loadTodaySummaryCached(Long userId, Scope scope) {
        String key = RedisKeyConstants.PROGRESS_TODAY_CACHE + userId;
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(cached)) {
                return JSONUtil.toBean(cached, PatientProgressVO.DayLevel.class);
            }
        } catch (Exception e) {
            log.warn("读取今日进度缓存失败，回源计算：{}", e.getMessage());
        }
        PatientProgressVO.DayLevel dayLevel = computeTodaySummary(scope);
        try {
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(dayLevel), TODAY_CACHE_TTL);
        } catch (Exception e) {
            log.warn("写入今日进度缓存失败：{}", e.getMessage());
        }
        return dayLevel;
    }

    private PatientProgressVO.DayLevel computeTodaySummary(Scope scope) {
        PatientProgressVO.DayLevel day = new PatientProgressVO.DayLevel();
        LocalDate today = LocalDate.now();
        day.setDate(today.toString());
        if (scope.taskQuery() == null) {
            day.setDoneTasks(0);
            day.setTotalTasks(0);
            day.setPercent(BigDecimal.ZERO);
            return day;
        }
        List<PlanDailyTask> todayTasks = planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                .in(PlanDailyTask::getPlanItemId, scope.itemMap().keySet())
                .eq(PlanDailyTask::getTaskDate, today));
        List<PlanDailyTask> countable = todayTasks.stream()
                .filter(t -> !DailyTaskStatus.SKIPPED.getCode().equals(t.getStatus()))
                .toList();
        int done = (int) countable.stream()
                .filter(t -> DailyTaskStatus.COMPLETED.getCode().equals(t.getStatus()))
                .count();
        day.setDoneTasks(done);
        day.setTotalTasks(countable.size());
        day.setPercent(percent(done, countable.size()));
        return day;
    }

    private List<PatientTaskProgressVO> toProgressRows(Scope scope, List<PlanDailyTask> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        // 批量取有效达标次数（排除 INSUFFICIENT；撤回由逻辑删除自动排除）
        Map<Long, Integer> correctByTask = trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                        .in(TrainingRecord::getDailyTaskId,
                                tasks.stream().map(PlanDailyTask::getTaskId).toList())
                        .ne(TrainingRecord::getPoseQuality, PoseQuality.INSUFFICIENT.getCode())
                        .select(TrainingRecord::getDailyTaskId, TrainingRecord::getCorrectReps))
                .stream()
                .collect(Collectors.groupingBy(TrainingRecord::getDailyTaskId,
                        Collectors.summingInt(r -> r.getCorrectReps() != null ? r.getCorrectReps() : 0)));

        return tasks.stream().map(task -> {
            PlanItem item = scope.itemMap().get(task.getPlanItemId());
            PatientTaskProgressVO row = new PatientTaskProgressVO();
            row.setTaskId(task.getTaskId());
            row.setTaskDate(task.getTaskDate());
            if (item != null) {
                row.setPlanItemId(item.getPlanItemId());
                RehabPlan plan = scope.planMap().get(item.getPlanId());
                if (plan != null) {
                    row.setPlanId(plan.getPlanId());
                    row.setPlanName(plan.getName());
                }
                row.setActionName(scope.actionNameByItem().get(item.getActionVersionId()));
            }
            row.setTargetSets(task.getTargetSets());
            row.setTargetReps(task.getTargetReps());
            int correct = correctByTask.getOrDefault(task.getTaskId(), 0);
            row.setCorrectReps(correct);
            row.setStatus(task.getStatus());
            row.setStatusName(safeStatusName(task.getStatus()));
            int targetReps = task.getTargetReps() != null ? task.getTargetReps() : 0;
            row.setProgressPercent(percent(correct, targetReps));
            row.setCompletedAt(task.getCompletedAt());
            return row;
        }).toList();
    }

    private PatientTrendPointVO toTrendPoint(String key, List<TrainingRecord> records,
                                             Map<Long, Integer> targetRepsByTask) {
        PatientTrendPointVO point = new PatientTrendPointVO();
        point.setDate(key);
        point.setRecordCount(records.size());
        int totalReps = 0;
        int correctReps = 0;
        BigDecimal scoreSum = BigDecimal.ZERO;
        int scoreCount = 0;
        BigDecimal rateSum = BigDecimal.ZERO;
        int rateCount = 0;
        BigDecimal romSum = BigDecimal.ZERO;
        int romCount = 0;
        BigDecimal holdSum = BigDecimal.ZERO;
        int holdCount = 0;
        BigDecimal validSum = BigDecimal.ZERO;
        int validCount = 0;
        BigDecimal repairedSum = BigDecimal.ZERO;
        int repairedCount = 0;
        for (TrainingRecord record : records) {
            int total = record.getTotalReps() != null ? record.getTotalReps() : 0;
            int correct = record.getCorrectReps() != null ? record.getCorrectReps() : 0;
            totalReps += total;
            correctReps += correct;
            if (record.getTotalScore() != null) {
                scoreSum = scoreSum.add(record.getTotalScore());
                scoreCount++;
            }
            if (record.getAvgRom() != null) {
                romSum = romSum.add(record.getAvgRom());
                romCount++;
            }
            if (record.getHoldRatio() != null) {
                holdSum = holdSum.add(record.getHoldRatio());
                holdCount++;
            }
            if (record.getValidFrameRatio() != null) {
                validSum = validSum.add(record.getValidFrameRatio());
                validCount++;
            }
            if (record.getPoseRepairedRatio() != null) {
                repairedSum = repairedSum.add(record.getPoseRepairedRatio());
                repairedCount++;
            }
            int target = targetRepsByTask.getOrDefault(record.getDailyTaskId(), 0);
            if (target > 0) {
                rateSum = rateSum.add(BigDecimal.valueOf(Math.min(1.0, correct * 1.0 / target)));
                rateCount++;
            }
        }
        point.setTotalReps(totalReps);
        point.setCorrectReps(correctReps);
        point.setAvgScore(scoreCount > 0
                ? scoreSum.divide(BigDecimal.valueOf(scoreCount), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        point.setAvgCompletionRate(rateCount > 0
                ? rateSum.divide(BigDecimal.valueOf(rateCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        // P7 扩展维度：ROM/保持率/姿态质量（拍摄条件区分用）
        point.setAvgRom(romCount > 0
                ? romSum.divide(BigDecimal.valueOf(romCount), 1, RoundingMode.HALF_UP)
                : null);
        point.setAvgHoldRatio(holdCount > 0
                ? holdSum.divide(BigDecimal.valueOf(holdCount), 4, RoundingMode.HALF_UP)
                : null);
        point.setAvgValidFrameRatio(validCount > 0
                ? validSum.divide(BigDecimal.valueOf(validCount), 4, RoundingMode.HALF_UP)
                : null);
        point.setAvgPoseRepairedRatio(repairedCount > 0
                ? repairedSum.divide(BigDecimal.valueOf(repairedCount), 4, RoundingMode.HALF_UP)
                : null);
        return point;
    }

    /** 当前患者在指定计划（可选）下的全部每日任务ID。 */
    private List<Long> listMyTaskIds(Long userId, Long planId) {
        LambdaQueryWrapper<RehabPlan> planQuery = new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getPatientId, userId)
                .select(RehabPlan::getPlanId);
        if (planId != null) {
            planQuery.eq(RehabPlan::getPlanId, planId);
        }
        List<Long> planIds = rehabPlanMapper.selectList(planQuery)
                .stream().map(RehabPlan::getPlanId).collect(Collectors.toList());
        if (planIds.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                        .in(PlanItem::getPlanId, planIds)
                        .select(PlanItem::getPlanItemId))
                .stream().map(PlanItem::getPlanItemId).collect(Collectors.toList());
        if (itemIds.isEmpty()) {
            return List.of();
        }
        return planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                        .in(PlanDailyTask::getPlanItemId, itemIds)
                        .select(PlanDailyTask::getTaskId))
                .stream().map(PlanDailyTask::getTaskId).collect(Collectors.toList());
    }

    private String safeStatusName(String code) {
        try {
            return DailyTaskStatus.fromCode(code).getName();
        } catch (IllegalArgumentException e) {
            return code;
        }
    }

    /** 百分比（0-100，1 位小数）；分母 <= 0 时返回 0。 */
    private BigDecimal percent(int done, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.min(100.0, done * 100.0 / total)).setScale(1, RoundingMode.HALF_UP);
    }
}
