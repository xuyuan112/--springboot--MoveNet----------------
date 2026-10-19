package com.rehab.app.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.app.service.AiQaService;
import com.rehab.app.service.ReportGenerateService;
import com.rehab.model.entity.AssessmentReport;
import com.rehab.model.entity.PlanDailyTask;
import com.rehab.model.entity.PlanItem;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.TrainingErrorLog;
import com.rehab.model.entity.TrainingRecord;
import com.rehab.model.enums.DailyTaskStatus;
import com.rehab.model.enums.PoseQuality;
import com.rehab.model.enums.ReportStatus;
import com.rehab.model.mapper.plan.PlanDailyTaskMapper;
import com.rehab.model.mapper.plan.PlanItemMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.training.AssessmentReportMapper;
import com.rehab.model.mapper.training.TrainingErrorLogMapper;
import com.rehab.model.mapper.training.TrainingRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评估报告预生成服务实现（P5-B5，调度第三步）。
 *
 * <p>周期口径：上一个完整 ISO 周（周一至周日）。对周期内产生过每日任务的全部计划
 * 聚合生成 DRAFT 报告（data_snapshot 固化统计），AI 小结成功时升为 AI_GENERATED；
 * 唯一约束 uk_report_period 保证幂等，重复执行跳过。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerateServiceImpl implements ReportGenerateService {

    private final PlanDailyTaskMapper planDailyTaskMapper;
    private final PlanItemMapper planItemMapper;
    private final RehabPlanMapper rehabPlanMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final TrainingErrorLogMapper trainingErrorLogMapper;
    private final AssessmentReportMapper assessmentReportMapper;
    private final AiQaService aiQaService;

    @Override
    public int generatePreviousWeekReports() {
        LocalDate periodEnd = LocalDate.now().with(DayOfWeek.MONDAY).minusDays(1);
        LocalDate periodStart = periodEnd.minusDays(6);
        List<PlanDailyTask> tasks = planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                .ge(PlanDailyTask::getTaskDate, periodStart)
                .le(PlanDailyTask::getTaskDate, periodEnd));
        if (tasks.isEmpty()) {
            log.info("[报告预生成] 周期 {} ~ {} 无任务，跳过", periodStart, periodEnd);
            return 0;
        }
        Map<Long, PlanItem> itemMap = planItemMapper.selectBatchIds(
                        tasks.stream().map(PlanDailyTask::getPlanItemId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(PlanItem::getPlanItemId, Function.identity()));

        // 任务按计划分组（无法归属计划项的任务跳过）
        Map<Long, List<PlanDailyTask>> tasksByPlan = new LinkedHashMap<>();
        for (PlanDailyTask task : tasks) {
            PlanItem item = itemMap.get(task.getPlanItemId());
            if (item == null) {
                continue;
            }
            tasksByPlan.computeIfAbsent(item.getPlanId(), k -> new ArrayList<>()).add(task);
        }
        if (tasksByPlan.isEmpty()) {
            return 0;
        }
        Map<Long, RehabPlan> planMap = rehabPlanMapper.selectBatchIds(tasksByPlan.keySet()).stream()
                .collect(Collectors.toMap(RehabPlan::getPlanId, Function.identity()));

        // 周期内全部训练记录与纠错日志（一次批量取出，内存分组）
        List<Long> allTaskIds = tasks.stream().map(PlanDailyTask::getTaskId).toList();
        Map<Long, List<TrainingRecord>> recordsByTask = trainingRecordMapper.selectList(
                        new LambdaQueryWrapper<TrainingRecord>()
                                .in(TrainingRecord::getDailyTaskId, allTaskIds))
                .stream()
                .collect(Collectors.groupingBy(TrainingRecord::getDailyTaskId));
        List<TrainingRecord> allRecords = recordsByTask.values().stream()
                .flatMap(List::stream).toList();
        Map<Long, Map<String, Long>> errorDimsByRecord = allRecords.isEmpty() ? Map.of()
                : trainingErrorLogMapper.selectList(new LambdaQueryWrapper<TrainingErrorLog>()
                                .in(TrainingErrorLog::getRecordId,
                                        allRecords.stream().map(TrainingRecord::getRecordId).toList())
                                .select(TrainingErrorLog::getRecordId, TrainingErrorLog::getDimension))
                        .stream()
                        .collect(Collectors.groupingBy(TrainingErrorLog::getRecordId,
                                Collectors.groupingBy(e -> e.getDimension() == null ? "UNKNOWN" : e.getDimension(),
                                        Collectors.counting())));

        int created = 0;
        for (Map.Entry<Long, List<PlanDailyTask>> entry : tasksByPlan.entrySet()) {
            Long planId = entry.getKey();
            RehabPlan plan = planMap.get(planId);
            if (plan == null) {
                continue;
            }
            Long exists = assessmentReportMapper.selectCount(new LambdaQueryWrapper<AssessmentReport>()
                    .eq(AssessmentReport::getPlanId, planId)
                    .eq(AssessmentReport::getPeriodStart, periodStart)
                    .eq(AssessmentReport::getPeriodEnd, periodEnd));
            if (exists != null && exists > 0) {
                continue;
            }
            try {
                created += generateForPlan(plan, entry.getValue(), recordsByTask, errorDimsByRecord,
                        periodStart, periodEnd, 1);
            } catch (DuplicateKeyException e) {
                // 并发/重复调度：唯一约束兜底，跳过
                log.info("[报告预生成] 计划 {} 周期 {} 报告已存在（唯一约束命中），跳过", planId, periodStart);
            } catch (Exception e) {
                // 单计划失败不影响其他计划
                log.error("[报告预生成] 计划 {} 周期 {} 生成失败", planId, periodStart, e);
            }
        }
        log.info("[报告预生成] 周期 {} ~ {} 完成，新建报告数={}", periodStart, periodEnd, created);
        return created;
    }

    // ==================== 私有辅助 ====================

    @Override
    public Long regenerateForPlan(Long planId, LocalDate periodStart, LocalDate periodEnd) {
        RehabPlan plan = rehabPlanMapper.selectById(planId);
        if (plan == null) {
            return null;
        }
        // 该计划在该周期产生的全部每日任务（冻结数据源：任务 + 训练记录快照）
        List<Long> itemIds = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                        .eq(PlanItem::getPlanId, planId).select(PlanItem::getPlanItemId))
                .stream().map(PlanItem::getPlanItemId).toList();
        List<PlanDailyTask> planTasks = itemIds.isEmpty() ? List.of()
                : planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                        .in(PlanDailyTask::getPlanItemId, itemIds)
                        .ge(PlanDailyTask::getTaskDate, periodStart)
                        .le(PlanDailyTask::getTaskDate, periodEnd));
        List<Long> taskIds = planTasks.stream().map(PlanDailyTask::getTaskId).toList();
        Map<Long, List<TrainingRecord>> recordsByTask = taskIds.isEmpty() ? Map.of()
                : trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                        .in(TrainingRecord::getDailyTaskId, taskIds))
                .stream().collect(Collectors.groupingBy(TrainingRecord::getDailyTaskId));
        List<TrainingRecord> allRecords = recordsByTask.values().stream()
                .flatMap(List::stream).toList();
        Map<Long, Map<String, Long>> errorDimsByRecord = allRecords.isEmpty() ? Map.of()
                : trainingErrorLogMapper.selectList(new LambdaQueryWrapper<TrainingErrorLog>()
                        .in(TrainingErrorLog::getRecordId,
                                allRecords.stream().map(TrainingRecord::getRecordId).toList())
                        .select(TrainingErrorLog::getRecordId, TrainingErrorLog::getDimension))
                .stream()
                .collect(Collectors.groupingBy(TrainingErrorLog::getRecordId,
                        Collectors.groupingBy(e -> e.getDimension() == null ? "UNKNOWN" : e.getDimension(),
                                Collectors.counting())));
        // 版本号 = 当前最大版本 + 1（原版本保留）
        Integer maxVersion = assessmentReportMapper.selectList(new LambdaQueryWrapper<AssessmentReport>()
                        .eq(AssessmentReport::getPlanId, planId)
                        .eq(AssessmentReport::getPeriodStart, periodStart)
                        .eq(AssessmentReport::getPeriodEnd, periodEnd)
                        .select(AssessmentReport::getVersionNo))
                .stream().map(AssessmentReport::getVersionNo)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        AssessmentReport report = buildReport(plan, planTasks, recordsByTask, errorDimsByRecord,
                periodStart, periodEnd, maxVersion + 1);
        assessmentReportMapper.insert(report);
        log.info("[报告重生成] 计划 {} 周期 {} ~ {} 新版本 v{} reportId={}",
                planId, periodStart, periodEnd, report.getVersionNo(), report.getReportId());
        return report.getReportId();
    }

    private int generateForPlan(RehabPlan plan, List<PlanDailyTask> planTasks,
                                Map<Long, List<TrainingRecord>> recordsByTask,
                                Map<Long, Map<String, Long>> errorDimsByRecord,
                                LocalDate periodStart, LocalDate periodEnd, int versionNo) {
        AssessmentReport report = buildReport(plan, planTasks, recordsByTask, errorDimsByRecord,
                periodStart, periodEnd, versionNo);
        assessmentReportMapper.insert(report);
        log.info("[报告预生成] 计划 {}（{}）周期 {} ~ {} 报告已生成 status={}",
                plan.getPlanId(), plan.getName(), periodStart, periodEnd, report.getStatus());
        return 1;
    }

    /** 由冻结数据构建报告实体（不含插入）。 */
    private AssessmentReport buildReport(RehabPlan plan, List<PlanDailyTask> planTasks,
                                         Map<Long, List<TrainingRecord>> recordsByTask,
                                         Map<Long, Map<String, Long>> errorDimsByRecord,
                                         LocalDate periodStart, LocalDate periodEnd, int versionNo) {
        List<TrainingRecord> planRecords = planTasks.stream()
                .map(t -> recordsByTask.getOrDefault(t.getTaskId(), List.of()))
                .flatMap(List::stream)
                .toList();
        List<TrainingRecord> validRecords = planRecords.stream()
                .filter(r -> !PoseQuality.INSUFFICIENT.getCode().equals(r.getPoseQuality()))
                .toList();

        long completed = planTasks.stream()
                .filter(t -> DailyTaskStatus.COMPLETED.getCode().equals(t.getStatus())).count();
        long expired = planTasks.stream()
                .filter(t -> DailyTaskStatus.EXPIRED.getCode().equals(t.getStatus())).count();
        long skipped = planTasks.stream()
                .filter(t -> DailyTaskStatus.SKIPPED.getCode().equals(t.getStatus())).count();

        int totalReps = 0;
        int correctReps = 0;
        BigDecimal scoreSum = BigDecimal.ZERO;
        int scoreCount = 0;
        Map<String, BigDecimal[]> scoreAcc = new LinkedHashMap<>();
        for (TrainingRecord record : validRecords) {
            totalReps += record.getTotalReps() != null ? record.getTotalReps() : 0;
            correctReps += record.getCorrectReps() != null ? record.getCorrectReps() : 0;
            if (record.getTotalScore() != null) {
                scoreSum = scoreSum.add(record.getTotalScore());
                scoreCount++;
            }
            accumulateScoreDetail(scoreAcc, record.getScoreDetail());
        }

        JSONObject snapshot = new JSONObject();
        snapshot.set("planId", plan.getPlanId());
        snapshot.set("planName", plan.getName());
        snapshot.set("planVersion", plan.getVersionNo());
        snapshot.set("periodStart", periodStart.toString());
        snapshot.set("periodEnd", periodEnd.toString());
        snapshot.set("totalTasks", planTasks.size());
        snapshot.set("completedTasks", (int) completed);
        snapshot.set("expiredTasks", (int) expired);
        snapshot.set("skippedTasks", (int) skipped);
        snapshot.set("completionRate", percent((int) completed, planTasks.size()));
        snapshot.set("recordCount", planRecords.size());
        snapshot.set("insufficientCount", planRecords.size() - validRecords.size());
        snapshot.set("totalReps", totalReps);
        snapshot.set("correctReps", correctReps);
        snapshot.set("avgTotalScore", scoreCount > 0
                ? scoreSum.divide(BigDecimal.valueOf(scoreCount), 1, RoundingMode.HALF_UP)
                : null);
        snapshot.set("scoreAverages", toScoreAverages(scoreAcc));
        snapshot.set("qualityDistribution", qualityDistribution(planRecords));
        Map<String, Long> errorDims = new LinkedHashMap<>();
        for (TrainingRecord record : planRecords) {
            errorDimsByRecord.getOrDefault(record.getRecordId(), Map.of())
                    .forEach((dim, count) -> errorDims.merge(dim, count, Long::sum));
        }
        snapshot.set("errorDimensionDistribution", errorDims);

        AssessmentReport report = new AssessmentReport();
        report.setPlanId(plan.getPlanId());
        report.setPeriodStart(periodStart);
        report.setPeriodEnd(periodEnd);
        report.setVersionNo(versionNo);
        report.setDataSnapshot(snapshot.toString());
        String aiDraft = aiQaService.generateReportDraft(snapshot);
        report.setAiDraft(aiDraft);
        report.setStatus(StringUtils.hasText(aiDraft)
                ? ReportStatus.AI_GENERATED.getCode()
                : ReportStatus.DRAFT.getCode());
        return report;
    }

    /** 累加单条记录的分项评分明细（JSON 列解析失败跳过该条）。 */
    private void accumulateScoreDetail(Map<String, BigDecimal[]> acc, String scoreDetailJson) {
        if (scoreDetailJson == null || scoreDetailJson.isBlank()) {
            return;
        }
        try {
            JSONObject detail = JSONUtil.parseObj(scoreDetailJson);
            for (String key : detail.keySet()) {
                BigDecimal value = detail.getBigDecimal(key);
                if (value == null) {
                    continue;
                }
                BigDecimal[] slot = acc.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                slot[0] = slot[0].add(value);
                slot[1] = slot[1].add(BigDecimal.ONE);
            }
        } catch (Exception ignored) {
            // 单条评分明细损坏不影响整体报告
        }
    }

    private JSONObject toScoreAverages(Map<String, BigDecimal[]> acc) {
        JSONObject result = new JSONObject();
        acc.forEach((key, slot) -> {
            if (slot[1].compareTo(BigDecimal.ZERO) > 0) {
                result.set(key, slot[0].divide(slot[1], 1, RoundingMode.HALF_UP));
            }
        });
        return result;
    }

    private JSONObject qualityDistribution(List<TrainingRecord> records) {
        Map<String, Long> counts = records.stream()
                .collect(Collectors.groupingBy(r -> r.getPoseQuality() == null ? "UNKNOWN" : r.getPoseQuality(),
                        Collectors.counting()));
        JSONObject result = new JSONObject();
        counts.forEach(result::set);
        return result;
    }

    /** 百分比（0-100，1 位小数）；分母 <= 0 返回 0。 */
    private BigDecimal percent(int done, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.min(100.0, done * 100.0 / total)).setScale(1, RoundingMode.HALF_UP);
    }
}
