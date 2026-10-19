package com.rehab.app.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.TrainingRecordService;
import com.rehab.app.vo.ErrorGroupVO;
import com.rehab.app.vo.ErrorLogVO;
import com.rehab.app.vo.TrainingRecordDetailVO;
import com.rehab.app.vo.TrainingRecordSubmitVO;
import com.rehab.app.vo.TrainingRecordVO;
import com.rehab.common.constant.RedisKeyConstants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.dto.TrainingRecordSubmitRequest;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.ActionRule;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.entity.AssessmentReport;
import com.rehab.model.entity.PlanDailyTask;
import com.rehab.model.entity.PlanItem;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.TrainingErrorLog;
import com.rehab.model.entity.TrainingRecord;
import com.rehab.model.enums.DailyTaskStatus;
import com.rehab.model.enums.PoseQuality;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.action.ActionRuleMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.model.mapper.plan.PlanDailyTaskMapper;
import com.rehab.model.mapper.plan.PlanItemMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.training.AssessmentReportMapper;
import com.rehab.model.mapper.training.TrainingErrorLogMapper;
import com.rehab.model.mapper.training.TrainingRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 患者端训练记录服务实现（P4-B2）。
 *
 * <p>核心约束（接口文档 4 / 需求 3.3）：</p>
 * <ul>
 *   <li>五重归属校验：每日任务 → 计划项 → 计划 → 患者 → 动作版本，逐环校验；</li>
 *   <li>{@code client_request_id} 幂等：唯一约束 (daily_task_id, client_request_id)，
 *       重复提交返回首次结果并带 duplicate 标记；</li>
 *   <li>{@code INSUFFICIENT} 记录保留但不计分：不计入任务完成数与趋势；</li>
 *   <li>事务内同时写训练记录、纠错日志并推进任务进度（需求 9 安全要求）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingRecordServiceImpl implements TrainingRecordService {

    /** 评分明细必填维度（similarity 在模板缺失时允许缺省按 0 处理） */
    private static final Set<String> REQUIRED_SCORE_KEYS =
            Set.of("amplitude", "hold", "rhythm", "stability", "symmetry");
    private static final String OPTIONAL_SCORE_KEY = "similarity";

    /** validFrameRatioMin 缺省阈值（与 DEFAULT_ALGO_CONFIG 一致） */
    private static final BigDecimal DEFAULT_VALID_RATIO_MIN = new BigDecimal("0.7");
    /** repairRatioLimit 缺省阈值（与 DEFAULT_ALGO_CONFIG 一致） */
    private static final BigDecimal DEFAULT_REPAIR_RATIO_LIMIT = new BigDecimal("0.3");

    private final TrainingRecordMapper trainingRecordMapper;
    private final TrainingErrorLogMapper trainingErrorLogMapper;
    private final AssessmentReportMapper assessmentReportMapper;
    private final PlanDailyTaskMapper planDailyTaskMapper;
    private final PlanItemMapper planItemMapper;
    private final RehabPlanMapper rehabPlanMapper;
    private final ActionVersionMapper actionVersionMapper;
    private final ActionMapper actionMapper;
    private final ActionRuleMapper actionRuleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrainingRecordSubmitVO submitTrainingRecord(TrainingRecordSubmitRequest request) {
        Long userId = SecurityUtils.getUserId();

        // ===== 五重归属校验：任务 → 计划项 → 计划 → 患者 → 动作版本 =====
        PlanDailyTask task = planDailyTaskMapper.selectById(request.getDailyTaskId());
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        PlanItem item = planItemMapper.selectById(task.getPlanItemId());
        if (item == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务对应的计划项不存在");
        }
        RehabPlan plan = rehabPlanMapper.selectById(item.getPlanId());
        if (plan == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND, "计划项对应的计划不存在");
        }
        if (!plan.getPatientId().equals(userId)) {
            // 验收标准：他人 daily_task_id 提交返回 403 OWNERSHIP_DENIED
            throw new BusinessException(ResultCode.OWNERSHIP_DENIED, "训练任务不属于当前用户");
        }
        if (!Objects.equals(request.getPlanItemId(), task.getPlanItemId())) {
            throw new BusinessException(ResultCode.OWNERSHIP_DENIED, "计划项与每日任务不匹配");
        }
        if (!Objects.equals(request.getActionVersionId(), item.getActionVersionId())) {
            throw new BusinessException(ResultCode.OWNERSHIP_DENIED, "动作版本与计划项不匹配");
        }
        ActionVersion version = actionVersionMapper.selectById(request.getActionVersionId());
        if (version == null) {
            throw new BusinessException(ResultCode.VERSION_NOT_FOUND);
        }

        // ===== 指标范围校验（422 METRIC_INVALID） =====
        validateMetrics(request);

        // ===== 纠错明细规则归属校验：ruleId 必须属于该动作版本（否则 FK 报 500，语义上应为 422） =====
        validateErrorRuleOwnership(request);

        // ===== 引擎一致性（422 ENGINE_VERSION_MISMATCH） =====
        String serverEngineId = version.getPoseEngineId();
        if (serverEngineId != null && !serverEngineId.isBlank()
                && !serverEngineId.trim().equals(request.getPoseEngineId().trim())) {
            throw new BusinessException(ResultCode.ENGINE_VERSION_MISMATCH,
                    "姿态引擎版本不一致：端侧 " + request.getPoseEngineId().trim()
                            + "，动作版本要求 " + serverEngineId.trim());
        }

        // ===== INSUFFICIENT 判定：端侧上报 + 服务端按动作版本阈值兜底复算 =====
        PoseQuality quality = deriveQuality(request, version.getAlgoConfig());

        // ===== 幂等：相同 (dailyTaskId, clientRequestId) 返回首次结果 =====
        TrainingRecord existing = trainingRecordMapper.selectOne(new LambdaQueryWrapper<TrainingRecord>()
                .eq(TrainingRecord::getDailyTaskId, request.getDailyTaskId())
                .eq(TrainingRecord::getClientRequestId, request.getClientRequestId())
                .last("limit 1"));
        if (existing != null) {
            TrainingRecordSubmitVO vo = buildSubmitVO(existing.getRecordId(), task, duplicateVO(true));
            vo.setInsufficient(PoseQuality.INSUFFICIENT.getCode().equals(existing.getPoseQuality()));
            return vo;
        }

        TrainingRecord record = new TrainingRecord();
        record.setDailyTaskId(request.getDailyTaskId());
        record.setActionVersionId(request.getActionVersionId());
        record.setStartedAt(request.getStartedAt());
        record.setEndedAt(request.getEndedAt());
        record.setTotalReps(request.getTotalReps());
        record.setCorrectReps(request.getCorrectReps());
        record.setTotalScore(request.getTotalScore());
        record.setScoreDetail(JSONUtil.toJsonStr(normalizeScoreDetail(request.getScoreDetail())));
        record.setAvgRom(request.getAvgRom());
        record.setPeakAngle(request.getPeakAngle());
        record.setTroughAngle(request.getTroughAngle());
        record.setPeakFrameNo(request.getPeakFrameNo());
        record.setTroughFrameNo(request.getTroughFrameNo());
        record.setHoldRatio(request.getHoldRatio());
        record.setTemplateSimilarity(request.getTemplateSimilarity());
        record.setValidFrameRatio(request.getValidFrameRatio());
        record.setPoseRepairedRatio(request.getPoseRepairedRatio());
        record.setStabilityScore(request.getStabilityScore());
        record.setSymmetryScore(request.getSymmetryScore());
        record.setPoseQuality(quality.getCode());
        record.setPoseEngineId(request.getPoseEngineId().trim());
        record.setClientRequestId(request.getClientRequestId().trim());
        record.setRemark(request.getRemark());
        try {
            trainingRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发同键提交：唯一约束兜底，回查首次结果
            TrainingRecord winner = trainingRecordMapper.selectOne(new LambdaQueryWrapper<TrainingRecord>()
                    .eq(TrainingRecord::getDailyTaskId, request.getDailyTaskId())
                    .eq(TrainingRecord::getClientRequestId, request.getClientRequestId())
                    .last("limit 1"));
            if (winner != null) {
                TrainingRecordSubmitVO vo = buildSubmitVO(winner.getRecordId(), task, duplicateVO(true));
                vo.setInsufficient(PoseQuality.INSUFFICIENT.getCode().equals(winner.getPoseQuality()));
                return vo;
            }
            throw e;
        }

        // ===== 纠错日志（同一事务） =====
        insertErrorLogs(record.getRecordId(), request);

        // ===== 任务进度推进（同一事务；INSUFFICIENT 不计分不推进） =====
        boolean insufficient = PoseQuality.INSUFFICIENT == quality;
        if (!insufficient) {
            advanceTaskProgress(task, request);
        }

        TrainingRecordSubmitVO vo = buildSubmitVO(record.getRecordId(), task, duplicateVO(false));
        vo.setInsufficient(insufficient);
        // 今日进度聚合缓存写后失效（P5-B1）
        evictProgressCache(userId);
        return vo;
    }

    @Override
    public PageResult<TrainingRecordVO> listMyRecords(Integer page, Integer size, LocalDate from, LocalDate to,
                                                      Long planId, Long actionVersionId) {
        Long userId = SecurityUtils.getUserId();
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        List<Long> taskIds = listMyTaskIds(userId, planId);
        if (taskIds.isEmpty()) {
            return PageResult.empty(page.longValue(), size.longValue());
        }
        // 注意：MP 条件实参为急切求值，null 日期必须先归一为 null 时间再交给条件包装
        LocalDateTime fromTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toTime = to != null ? to.atTime(23, 59, 59) : null;
        Page<TrainingRecord> mpPage = new Page<>(page, size);
        trainingRecordMapper.selectPage(mpPage, new LambdaQueryWrapper<TrainingRecord>()
                .in(TrainingRecord::getDailyTaskId, taskIds)
                .ge(fromTime != null, TrainingRecord::getStartedAt, fromTime)
                .le(toTime != null, TrainingRecord::getStartedAt, toTime)
                .eq(actionVersionId != null, TrainingRecord::getActionVersionId, actionVersionId)
                .orderByDesc(TrainingRecord::getStartedAt)
                .orderByDesc(TrainingRecord::getRecordId));
        return PageResult.of(mpPage, toVOList(mpPage.getRecords()));
    }

    @Override
    public TrainingRecordDetailVO getMyRecordDetail(Long recordId) {
        TrainingRecord record = requireOwnRecord(recordId);
        TrainingRecordDetailVO vo = new TrainingRecordDetailVO();
        copyBase(record, vo);
        vo.setScoreDetail(parseScoreDetail(record.getScoreDetail()));
        vo.setAvgRom(record.getAvgRom());
        vo.setPeakAngle(record.getPeakAngle());
        vo.setTroughAngle(record.getTroughAngle());
        vo.setPeakFrameNo(record.getPeakFrameNo());
        vo.setTroughFrameNo(record.getTroughFrameNo());
        vo.setHoldRatio(record.getHoldRatio());
        vo.setTemplateSimilarity(record.getTemplateSimilarity());
        vo.setStabilityScore(record.getStabilityScore());
        vo.setSymmetryScore(record.getSymmetryScore());
        vo.setPoseEngineId(record.getPoseEngineId());
        vo.setRemark(record.getRemark());
        List<ErrorLogVO> errorLogs = trainingErrorLogMapper.selectList(new LambdaQueryWrapper<TrainingErrorLog>()
                        .eq(TrainingErrorLog::getRecordId, recordId)
                        .orderByAsc(TrainingErrorLog::getErrorId))
                .stream().map(this::toErrorLogVO).collect(Collectors.toList());
        vo.setErrorLogs(errorLogs);
        vo.setErrorSummary(groupByDimension(errorLogs));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawRecord(Long recordId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "撤回必须填写原因");
        }
        TrainingRecord record = requireOwnRecord(recordId);

        // 已纳入评估报告统计周期的记录不可撤回（报告按 plan + period 聚合，P5 生成后自动生效）
        PlanItem item = planItemMapper.selectById(
                planDailyTaskMapper.selectById(record.getDailyTaskId()).getPlanItemId());
        LocalDate recordDate = record.getStartedAt().toLocalDate();
        Long reportCount = assessmentReportMapper.selectCount(new LambdaQueryWrapper<AssessmentReport>()
                .eq(AssessmentReport::getPlanId, item.getPlanId())
                .le(AssessmentReport::getPeriodStart, recordDate)
                .ge(AssessmentReport::getPeriodEnd, recordDate));
        if (reportCount != null && reportCount > 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "该记录已纳入评估报告统计周期，不可撤回");
        }

        PlanDailyTask task = planDailyTaskMapper.selectById(record.getDailyTaskId());
        trainingRecordMapper.update(null, new LambdaUpdateWrapper<TrainingRecord>()
                .eq(TrainingRecord::getRecordId, recordId)
                .set(TrainingRecord::getDeleted, 1)
                .set(TrainingRecord::getWithdrawReason, reason.trim())
                .set(TrainingRecord::getWithdrawnAt, LocalDateTime.now()));

        // 撤回后任务进度由查询侧按有效记录重算；若此前因该记录完成，则回退为进行中
        if (task != null && DailyTaskStatus.COMPLETED.getCode().equals(task.getStatus())) {
            int validSum = sumValidCorrectReps(task.getTaskId());
            int targetReps = task.getTargetReps() != null ? task.getTargetReps() : 0;
            if (targetReps <= 0 || validSum < targetReps) {
                task.setStatus(DailyTaskStatus.IN_PROGRESS.getCode());
                task.setCompletedAt(null);
                planDailyTaskMapper.update(task, new LambdaUpdateWrapper<PlanDailyTask>()
                        .eq(PlanDailyTask::getTaskId, task.getTaskId())
                        .set(PlanDailyTask::getStatus, task.getStatus())
                        .set(PlanDailyTask::getCompletedAt, null));
            }
        }
        // 今日进度聚合缓存写后失效（P5-B1）
        evictProgressCache(SecurityUtils.getUserId());
    }

    // ==================== 私有辅助 ====================

    /** 失效患者今日进度聚合缓存（cache:progress:today:{userId}），Redis 异常不阻断业务。 */
    private void evictProgressCache(Long userId) {
        try {
            stringRedisTemplate.delete(RedisKeyConstants.PROGRESS_TODAY_CACHE + userId);
        } catch (Exception e) {
            log.warn("失效今日进度缓存失败（TTL 兜底过期）：{}", e.getMessage());
        }
    }

    private void validateMetrics(TrainingRecordSubmitRequest request) {
        if (request.getEndedAt().isBefore(request.getStartedAt())) {
            throw new BusinessException(ResultCode.METRIC_INVALID, "结束时间不能早于开始时间");
        }
        if (request.getCorrectReps() > request.getTotalReps()) {
            throw new BusinessException(ResultCode.METRIC_INVALID, "达标次数不能大于总次数");
        }
        Map<String, BigDecimal> detail = request.getScoreDetail();
        for (String key : REQUIRED_SCORE_KEYS) {
            if (!detail.containsKey(key) || detail.get(key) == null) {
                throw new BusinessException(ResultCode.METRIC_INVALID, "评分明细缺少维度：" + key);
            }
        }
        BigDecimal similarity = detail.getOrDefault(OPTIONAL_SCORE_KEY, BigDecimal.ZERO);
        for (Map.Entry<String, BigDecimal> entry : detail.entrySet()) {
            BigDecimal value = entry.getValue();
            if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException(ResultCode.METRIC_INVALID,
                        "评分维度 " + entry.getKey() + " 需在 0~100 之间");
            }
        }
        detail.put(OPTIONAL_SCORE_KEY, similarity);
    }

    /** 服务端兜底复算 INSUFFICIENT：有效帧占比低于阈值或修复占比超限（阈值取自动作版本 algo_config）。 */
    private PoseQuality deriveQuality(TrainingRecordSubmitRequest request, String algoConfigJson) {
        if (PoseQuality.INSUFFICIENT == request.getPoseQuality()) {
            return PoseQuality.INSUFFICIENT;
        }
        JSONObject conf = parseAlgoConfig(algoConfigJson);
        BigDecimal validMin = conf.getBigDecimal("validFrameRatioMin", DEFAULT_VALID_RATIO_MIN);
        BigDecimal repairLimit = conf.getBigDecimal("repairRatioLimit", DEFAULT_REPAIR_RATIO_LIMIT);
        BigDecimal validRatio = request.getValidFrameRatio();
        BigDecimal repaired = request.getPoseRepairedRatio();
        if (validRatio != null && validRatio.compareTo(validMin) < 0) {
            return PoseQuality.INSUFFICIENT;
        }
        if (repaired != null && repaired.compareTo(repairLimit) > 0) {
            return PoseQuality.INSUFFICIENT;
        }
        return request.getPoseQuality();
    }

    private JSONObject parseAlgoConfig(String algoConfigJson) {
        if (algoConfigJson == null || algoConfigJson.isBlank()) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(algoConfigJson);
        } catch (Exception e) {
            log.warn("algo_config 解析失败，使用默认阈值：{}", e.getMessage());
            return new JSONObject();
        }
    }

    /** 补齐 similarity 缺省并返回规范化评分明细。 */
    private Map<String, Object> normalizeScoreDetail(Map<String, BigDecimal> detail) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        detail.forEach((k, v) -> normalized.put(k, v));
        normalized.putIfAbsent(OPTIONAL_SCORE_KEY, BigDecimal.ZERO);
        return normalized;
    }

    /** 校验纠错明细引用的 ruleId 均存在且属于当前动作版本（端侧快照自带，恶意/异常载荷返回 422）。 */
    private void validateErrorRuleOwnership(TrainingRecordSubmitRequest request) {
        List<TrainingRecordSubmitRequest.ErrorLogItem> items = request.getErrorSummary();
        if (items == null || items.isEmpty()) {
            return;
        }
        Set<Long> ruleIds = items.stream()
                .map(TrainingRecordSubmitRequest.ErrorLogItem::getRuleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ruleIds.isEmpty()) {
            return;
        }
        List<ActionRule> rules = actionRuleMapper.selectList(new LambdaQueryWrapper<ActionRule>()
                .in(ActionRule::getActionRuleId, ruleIds)
                .select(ActionRule::getActionRuleId, ActionRule::getActionVersionId));
        Map<Long, Long> versionByRuleId = rules.stream()
                .collect(Collectors.toMap(ActionRule::getActionRuleId, ActionRule::getActionVersionId));
        for (Long ruleId : ruleIds) {
            Long ownerVersionId = versionByRuleId.get(ruleId);
            if (ownerVersionId == null || !ownerVersionId.equals(request.getActionVersionId())) {
                throw new BusinessException(ResultCode.METRIC_INVALID,
                        "纠错明细规则不属于该动作版本：ruleId=" + ruleId);
            }
        }
    }

    private void insertErrorLogs(Long recordId, TrainingRecordSubmitRequest request) {
        List<TrainingRecordSubmitRequest.ErrorLogItem> items = request.getErrorSummary();
        if (items == null || items.isEmpty()) {
            return;
        }
        LocalDateTime startedAt = request.getStartedAt();
        for (TrainingRecordSubmitRequest.ErrorLogItem item : items) {
            TrainingErrorLog errorLog = new TrainingErrorLog();
            errorLog.setRecordId(recordId);
            errorLog.setRuleId(item.getRuleId());
            errorLog.setDimension(item.getDimension());
            errorLog.setPhase(item.getPhase());
            errorLog.setErrorType(item.getDimension());
            errorLog.setJointName(item.getJointName());
            errorLog.setDeviation(item.getDeviation());
            errorLog.setSeverity(item.getSeverity());
            errorLog.setFrameNo(item.getFrameNo());
            errorLog.setPromptText(item.getPromptText());
            LocalDateTime errorAt = startedAt;
            if (item.getOffsetSec() != null) {
                errorAt = startedAt.plusSeconds(item.getOffsetSec().longValue());
            }
            errorLog.setErrorAt(errorAt);
            trainingErrorLogMapper.insert(errorLog);
        }
    }

    /** 推进任务进度：仅 PENDING/IN_PROGRESS 可推进；达标置 COMPLETED。 */
    private void advanceTaskProgress(PlanDailyTask task, TrainingRecordSubmitRequest request) {
        String stored = task.getStatus();
        boolean mutable = DailyTaskStatus.PENDING.getCode().equals(stored)
                || DailyTaskStatus.IN_PROGRESS.getCode().equals(stored);
        if (!mutable) {
            // COMPLETED/SKIPPED/EXPIRED 不回改状态（EXPIRED 边界提交的记录保留但不复活任务）
            return;
        }
        if (DailyTaskStatus.PENDING.getCode().equals(stored)) {
            task.setStatus(DailyTaskStatus.IN_PROGRESS.getCode());
            if (task.getStartedAt() == null) {
                task.setStartedAt(request.getStartedAt());
            }
        }
        int validSum = sumValidCorrectReps(task.getTaskId());
        int targetReps = task.getTargetReps() != null ? task.getTargetReps() : 0;
        if (targetReps > 0 && validSum >= targetReps) {
            task.setStatus(DailyTaskStatus.COMPLETED.getCode());
            task.setCompletedAt(LocalDateTime.now());
        }
        planDailyTaskMapper.updateById(task);
    }

    /** 任务有效达标次数合计（排除 INSUFFICIENT；已撤回由逻辑删除自动排除）。 */
    private int sumValidCorrectReps(Long taskId) {
        List<TrainingRecord> records = trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                .eq(TrainingRecord::getDailyTaskId, taskId)
                .ne(TrainingRecord::getPoseQuality, PoseQuality.INSUFFICIENT.getCode())
                .select(TrainingRecord::getCorrectReps));
        return records.stream()
                .map(TrainingRecord::getCorrectReps)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
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
            return Collections.emptyList();
        }
        List<Long> itemIds = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                        .in(PlanItem::getPlanId, planIds)
                        .select(PlanItem::getPlanItemId))
                .stream().map(PlanItem::getPlanItemId).collect(Collectors.toList());
        if (itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                        .in(PlanDailyTask::getPlanItemId, itemIds)
                        .select(PlanDailyTask::getTaskId))
                .stream().map(PlanDailyTask::getTaskId).collect(Collectors.toList());
    }

    /** 归属校验：记录 → 任务 → 计划项 → 计划，患者必须为当前用户，否则统一 404（不泄露他人记录存在性）。 */
    private TrainingRecord requireOwnRecord(Long recordId) {
        TrainingRecord record = trainingRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "训练记录不存在");
        }
        PlanDailyTask task = planDailyTaskMapper.selectById(record.getDailyTaskId());
        PlanItem item = task == null ? null : planItemMapper.selectById(task.getPlanItemId());
        RehabPlan plan = item == null ? null : rehabPlanMapper.selectById(item.getPlanId());
        if (plan == null || !plan.getPatientId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "训练记录不存在");
        }
        return record;
    }

    private TrainingRecordSubmitVO duplicateVO(boolean duplicate) {
        TrainingRecordSubmitVO marker = new TrainingRecordSubmitVO();
        marker.setDuplicate(duplicate);
        return marker;
    }

    private TrainingRecordSubmitVO buildSubmitVO(Long recordId, PlanDailyTask task, TrainingRecordSubmitVO vo) {
        vo.setTrainingRecordId(recordId);
        vo.setDailyTaskId(task.getTaskId());
        vo.setTaskStatus(task.getStatus());
        int validSum = sumValidCorrectReps(task.getTaskId());
        vo.setCorrectReps(validSum);
        int targetReps = task.getTargetReps() != null ? task.getTargetReps() : 0;
        vo.setTargetReps(targetReps);
        vo.setProgressPercent(targetReps > 0
                ? BigDecimal.valueOf(Math.min(100.0, validSum * 100.0 / targetReps)).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        return vo;
    }

    private List<TrainingRecordVO> toVOList(List<TrainingRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream()
                .map(record -> {
                    TrainingRecordVO vo = new TrainingRecordVO();
                    copyBase(record, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private void copyBase(TrainingRecord record, TrainingRecordVO vo) {
        vo.setRecordId(record.getRecordId());
        vo.setDailyTaskId(record.getDailyTaskId());
        vo.setActionVersionId(record.getActionVersionId());
        vo.setStartedAt(record.getStartedAt());
        vo.setEndedAt(record.getEndedAt());
        vo.setTotalReps(record.getTotalReps());
        vo.setCorrectReps(record.getCorrectReps());
        vo.setTotalScore(record.getTotalScore());
        vo.setPoseQuality(record.getPoseQuality());
        vo.setValidFrameRatio(record.getValidFrameRatio());
        vo.setPoseRepairedRatio(record.getPoseRepairedRatio());
        vo.setCreatedAt(record.getCreatedAt());
        enrichPlanAndAction(record, vo);
    }

    /** 通过任务 → 计划项 → 计划/动作版本 → 动作补齐计划与动作名称。 */
    private void enrichPlanAndAction(TrainingRecord record, TrainingRecordVO vo) {
        PlanDailyTask task = planDailyTaskMapper.selectById(record.getDailyTaskId());
        if (task == null) {
            return;
        }
        PlanItem item = planItemMapper.selectById(task.getPlanItemId());
        if (item == null) {
            return;
        }
        vo.setPlanItemId(item.getPlanItemId());
        vo.setPlanId(item.getPlanId());
        RehabPlan plan = rehabPlanMapper.selectById(item.getPlanId());
        if (plan != null) {
            vo.setPlanName(plan.getName());
        }
        ActionVersion version = actionVersionMapper.selectById(record.getActionVersionId());
        if (version != null) {
            Action action = actionMapper.selectById(version.getActionId());
            if (action != null) {
                vo.setActionName(action.getName());
            }
        }
    }

    private Map<String, BigDecimal> parseScoreDetail(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            JSONObject obj = JSONUtil.parseObj(json);
            Map<String, BigDecimal> result = new LinkedHashMap<>();
            for (String key : obj.keySet()) {
                result.put(key, obj.getBigDecimal(key));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private ErrorLogVO toErrorLogVO(TrainingErrorLog errorLog) {
        ErrorLogVO vo = new ErrorLogVO();
        vo.setErrorId(errorLog.getErrorId());
        vo.setRuleId(errorLog.getRuleId());
        vo.setDimension(errorLog.getDimension());
        vo.setPhase(errorLog.getPhase());
        vo.setErrorType(errorLog.getErrorType());
        vo.setJointName(errorLog.getJointName());
        vo.setCurrentValue(errorLog.getCurrentValue());
        vo.setExpectedValue(errorLog.getExpectedValue());
        vo.setDeviation(errorLog.getDeviation());
        vo.setSeverity(errorLog.getSeverity());
        vo.setFrameNo(errorLog.getFrameNo());
        vo.setPromptText(errorLog.getPromptText());
        vo.setErrorAt(errorLog.getErrorAt());
        return vo;
    }

    private List<ErrorGroupVO> groupByDimension(List<ErrorLogVO> errorLogs) {
        Map<String, Long> counts = errorLogs.stream()
                .collect(Collectors.groupingBy(ErrorLogVO::getDimension, Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> {
                    ErrorGroupVO group = new ErrorGroupVO();
                    group.setDimension(e.getKey());
                    group.setCount(e.getValue());
                    return group;
                })
                .collect(Collectors.toList());
    }
}
