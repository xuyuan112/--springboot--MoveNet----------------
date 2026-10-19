package com.rehab.app.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.app.service.ReportGenerateService;
import com.rehab.common.constant.RedisKeyConstants;
import com.rehab.model.entity.PlanDailyTask;
import com.rehab.model.entity.PlanItem;
import com.rehab.model.entity.PlanSchedule;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.TrainingRecord;
import com.rehab.model.enums.DailyTaskStatus;
import com.rehab.model.enums.FrequencyType;
import com.rehab.model.enums.PlanStatus;
import com.rehab.model.mapper.plan.PlanDailyTaskMapper;
import com.rehab.model.mapper.plan.PlanItemMapper;
import com.rehab.model.mapper.plan.PlanScheduleMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.training.TrainingRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 每日任务定时调度（需求 3.2.2 与项目开发文档第 7 节）。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>每日 00:10：对 PUBLISHED 计划按 frequency_type + plan_schedule 生成当日 plan_daily_task，
 *       粒度=计划动作项×日期，以 (plan_item_id, task_date) 存在性校验保证幂等；</li>
 *   <li>每日 23:59:59：将当日仍为 PENDING/IN_PROGRESS 且无训练记录的任务置 EXPIRED；</li>
 *   <li>每日 01:00：报告预生成（P5-B5）——按上一个完整 ISO 周聚合生成 DRAFT 报告。</li>
 * </ul>
 *
 * <p>通过 Redis 锁（带日期后缀）保证多实例并发与重复触发不重复执行。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyTaskScheduleTask {

    /** 调度锁 TTL：30 分钟（足够一轮调度执行完毕，且崩死后可自动恢复） */
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    private final RehabPlanMapper rehabPlanMapper;
    private final PlanItemMapper planItemMapper;
    private final PlanScheduleMapper planScheduleMapper;
    private final PlanDailyTaskMapper planDailyTaskMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final ReportGenerateService reportGenerateService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成当日任务：每天 00:10 执行。
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void generateDailyTasks() {
        LocalDate today = LocalDate.now();
        String lockKey = RedisKeyConstants.TASK_SCHEDULE_GENERATE + today;
        String lockValue = UUID.randomUUID().toString();
        if (!acquireLock(lockKey, lockValue)) {
            log.info("[定时任务] generateDailyTasks 已有实例在执行，跳过（{}）", today);
            return;
        }
        try {
            int created = doGenerateDailyTasks(today);
            log.info("[定时任务] generateDailyTasks 完成，日期={}，新增任务数={}", today, created);
        } catch (Exception e) {
            log.error("[定时任务] generateDailyTasks 执行异常，日期={}", today, e);
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    /**
     * 过期任务置位：每天 23:59:59 执行。
     */
    @Scheduled(cron = "59 59 23 * * ?")
    public void expireDailyTasks() {
        LocalDate today = LocalDate.now();
        String lockKey = RedisKeyConstants.TASK_SCHEDULE_EXPIRE + today;
        String lockValue = UUID.randomUUID().toString();
        if (!acquireLock(lockKey, lockValue)) {
            log.info("[定时任务] expireDailyTasks 已有实例在执行，跳过（{}）", today);
            return;
        }
        try {
            int expired = doExpireDailyTasks(today);
            log.info("[定时任务] expireDailyTasks 完成，日期={}，置过期任务数={}", today, expired);
        } catch (Exception e) {
            log.error("[定时任务] expireDailyTasks 执行异常，日期={}", today, e);
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    /**
     * 报告预生成：每天 01:00 执行（P5-B5）。
     *
     * <p>按上一个完整 ISO 周聚合各计划任务/记录生成 DRAFT 报告；
     * 幂等由 uk_report_period 唯一约束保证，重复触发仅跳过。</p>
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void preGenerateReports() {
        LocalDate today = LocalDate.now();
        String lockKey = RedisKeyConstants.TASK_SCHEDULE_REPORT + today;
        String lockValue = UUID.randomUUID().toString();
        if (!acquireLock(lockKey, lockValue)) {
            log.info("[定时任务] preGenerateReports 已有实例在执行，跳过（{}）", today);
            return;
        }
        try {
            int created = reportGenerateService.generatePreviousWeekReports();
            log.info("[定时任务] preGenerateReports 完成，日期={}，新建报告数={}", today, created);
        } catch (Exception e) {
            log.error("[定时任务] preGenerateReports 执行异常，日期={}", today, e);
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    // ==================== 业务逻辑 ====================

    /**
     * 对当日有效的 PUBLISHED 计划逐动作项生成任务，返回新增任务数。
     */
    private int doGenerateDailyTasks(LocalDate today) {
        List<RehabPlan> plans = rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getStatus, PlanStatus.PUBLISHED.getCode())
                .le(RehabPlan::getStartDate, today)
                .ge(RehabPlan::getEndDate, today));
        if (plans.isEmpty()) {
            return 0;
        }
        int weekday = today.getDayOfWeek().getValue(); // 1=周一 ... 7=周日，与 plan_schedule.weekday 对齐
        int created = 0;
        for (RehabPlan plan : plans) {
            if (!shouldGenerateOn(plan, today, weekday)) {
                continue;
            }
            List<PlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                    .eq(PlanItem::getPlanId, plan.getPlanId()));
            for (PlanItem item : items) {
                // 幂等：同计划动作同日期唯一，已存在则跳过
                Long exists = planDailyTaskMapper.selectCount(new LambdaQueryWrapper<PlanDailyTask>()
                        .eq(PlanDailyTask::getPlanItemId, item.getPlanItemId())
                        .eq(PlanDailyTask::getTaskDate, today));
                if (exists != null && exists > 0) {
                    continue;
                }
                PlanDailyTask task = new PlanDailyTask();
                task.setPlanItemId(item.getPlanItemId());
                task.setTaskDate(today);
                // 目标参数快照：生成时从 plan_item 复制，不随后续计划调整变化
                task.setTargetSets(item.getTargetSets());
                task.setTargetReps(item.getTargetReps());
                task.setStatus(DailyTaskStatus.PENDING.getCode());
                planDailyTaskMapper.insert(task);
                created++;
            }
        }
        return created;
    }

    /**
     * 判断计划当日是否应生成任务：DAILY 每天；WEEKLY/CUSTOM 按 plan_schedule.weekday 匹配。
     */
    private boolean shouldGenerateOn(RehabPlan plan, LocalDate today, int weekday) {
        if (FrequencyType.DAILY.getCode().equals(plan.getFrequencyType())) {
            return true;
        }
        // WEEKLY / CUSTOM：当日星期需存在于 plan_schedule
        Long count = planScheduleMapper.selectCount(new LambdaQueryWrapper<PlanSchedule>()
                .eq(PlanSchedule::getPlanId, plan.getPlanId())
                .eq(PlanSchedule::getWeekday, weekday));
        return count != null && count > 0;
    }

    /**
     * 将当日仍为 PENDING/IN_PROGRESS 且无训练记录的任务置 EXPIRED，返回处理数。
     */
    private int doExpireDailyTasks(LocalDate today) {
        List<PlanDailyTask> tasks = planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                .eq(PlanDailyTask::getTaskDate, today)
                .in(PlanDailyTask::getStatus,
                        DailyTaskStatus.PENDING.getCode(), DailyTaskStatus.IN_PROGRESS.getCode()));
        if (tasks.isEmpty()) {
            return 0;
        }
        List<Long> taskIds = tasks.stream().map(PlanDailyTask::getTaskId).collect(Collectors.toList());
        // 有训练记录的任务不过期（患者已开始/部分完成，状态由查询侧按达标次数派生）
        Map<Long, Long> recordCountMap = trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                        .in(TrainingRecord::getDailyTaskId, taskIds)
                        .select(TrainingRecord::getDailyTaskId))
                .stream()
                .collect(Collectors.groupingBy(TrainingRecord::getDailyTaskId, Collectors.counting()));
        int expired = 0;
        for (PlanDailyTask task : tasks) {
            if (recordCountMap.containsKey(task.getTaskId())) {
                continue;
            }
            PlanDailyTask update = new PlanDailyTask();
            update.setTaskId(task.getTaskId());
            update.setStatus(DailyTaskStatus.EXPIRED.getCode());
            planDailyTaskMapper.updateById(update);
            expired++;
        }
        return expired;
    }

    // ==================== 锁 ====================

    private boolean acquireLock(String key, String value) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, value, LOCK_TTL);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            // Redis 不可用时降级为单实例直接执行（唯一约束兜底幂等）
            log.warn("[定时任务] 获取调度锁失败，降级直接执行：{}", e.getMessage());
            return true;
        }
    }

    /** 仅允许锁持有者释放（GET 校验后删除） */
    private void releaseLock(String key, String value) {
        try {
            String current = stringRedisTemplate.opsForValue().get(key);
            if (value.equals(current)) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("[定时任务] 调度锁释放异常：{}", e.getMessage());
        }
    }
}
