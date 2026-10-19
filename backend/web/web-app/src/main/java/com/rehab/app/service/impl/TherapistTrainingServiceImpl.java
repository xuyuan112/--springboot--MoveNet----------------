package com.rehab.app.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.PatientProgressService;
import com.rehab.app.service.TherapistTrainingService;
import com.rehab.app.vo.PatientProgressVO;
import com.rehab.app.vo.PatientTrendPointVO;
import com.rehab.app.vo.TherapistAnnotationVO;
import com.rehab.app.vo.TherapistErrorLogVO;
import com.rehab.app.vo.TherapistTrainingRecordDetailVO;
import com.rehab.app.vo.TherapistTrainingRecordVO;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.entity.PatientProfile;
import com.rehab.model.entity.PatientTherapist;
import com.rehab.model.entity.PlanDailyTask;
import com.rehab.model.entity.PlanItem;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.TrainingAnnotation;
import com.rehab.model.entity.TrainingErrorLog;
import com.rehab.model.entity.TrainingRecord;
import com.rehab.model.mapper.account.PatientProfileMapper;
import com.rehab.model.mapper.account.PatientTherapistMapper;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.model.mapper.plan.PlanDailyTaskMapper;
import com.rehab.model.mapper.plan.PlanItemMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.training.TrainingAnnotationMapper;
import com.rehab.model.mapper.training.TrainingErrorLogMapper;
import com.rehab.model.mapper.training.TrainingRecordMapper;
import com.rehab.model.enums.BindingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 治疗师端训练数据服务实现（P7-B1/P7-B3）。
 *
 * <p>口径：训练记录无患者冗余列，统一经 record→daily_task→plan_item→rehab_plan
 * 链路定位归属，plan.therapist_id=当前治疗师即本人负责；INSUFFICIENT 记录保留展示
 * （供区分"表现差"与"拍摄条件差"）；批注只增不改；导出仅汇总指标。</p>
 */
@Service
@RequiredArgsConstructor
public class TherapistTrainingServiceImpl implements TherapistTrainingService {

    private static final DateTimeFormatter CSV_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TrainingRecordMapper trainingRecordMapper;
    private final TrainingErrorLogMapper trainingErrorLogMapper;
    private final TrainingAnnotationMapper trainingAnnotationMapper;
    private final PlanDailyTaskMapper planDailyTaskMapper;
    private final PlanItemMapper planItemMapper;
    private final RehabPlanMapper rehabPlanMapper;
    private final ActionVersionMapper actionVersionMapper;
    private final ActionMapper actionMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final PatientTherapistMapper patientTherapistMapper;
    private final PatientProgressService patientProgressService;

    @Override
    public PageResult<TherapistTrainingRecordVO> listRecords(Integer page, Integer size, Long patientId, Long planId,
                                                             Long actionVersionId, LocalDate from, LocalDate to,
                                                             String poseQuality) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        // 记录范围 = 本人负责计划（可叠加患者/计划过滤）的任务集合
        List<Long> taskIds = listTaskIdsOfMyScope(patientId, planId);
        if (taskIds.isEmpty()) {
            return PageResult.empty(page.longValue(), size.longValue());
        }
        LocalDateTime fromTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toTime = to != null ? to.atTime(23, 59, 59) : null;
        Page<TrainingRecord> mpPage = new Page<>(page, size);
        trainingRecordMapper.selectPage(mpPage, new LambdaQueryWrapper<TrainingRecord>()
                .in(TrainingRecord::getDailyTaskId, taskIds)
                .eq(actionVersionId != null, TrainingRecord::getActionVersionId, actionVersionId)
                .eq(StringUtils.hasText(poseQuality), TrainingRecord::getPoseQuality, poseQuality)
                .ge(fromTime != null, TrainingRecord::getStartedAt, fromTime)
                .le(toTime != null, TrainingRecord::getStartedAt, toTime)
                .orderByDesc(TrainingRecord::getStartedAt)
                .orderByDesc(TrainingRecord::getRecordId));
        List<TherapistTrainingRecordVO> voList = toRecordVOs(mpPage.getRecords());
        return PageResult.of(mpPage, voList);
    }

    @Override
    public TherapistTrainingRecordDetailVO getRecordDetail(Long recordId) {
        TrainingRecord record = trainingRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "训练记录不存在");
        }
        RecordContext context = loadRecordContext(record);
        requireMyPlan(context.plan());

        TherapistTrainingRecordDetailVO vo = new TherapistTrainingRecordDetailVO();
        vo.setRecordId(record.getRecordId());
        vo.setDailyTaskId(record.getDailyTaskId());
        vo.setStartedAt(record.getStartedAt());
        vo.setEndedAt(record.getEndedAt());
        vo.setTotalReps(record.getTotalReps());
        vo.setCorrectReps(record.getCorrectReps());
        vo.setTotalScore(record.getTotalScore());
        vo.setScoreDetail(parseScoreDetail(record.getScoreDetail()));
        vo.setAvgRom(record.getAvgRom());
        vo.setPeakAngle(record.getPeakAngle());
        vo.setTroughAngle(record.getTroughAngle());
        vo.setHoldRatio(record.getHoldRatio());
        vo.setTemplateSimilarity(record.getTemplateSimilarity());
        vo.setValidFrameRatio(record.getValidFrameRatio());
        vo.setPoseRepairedRatio(record.getPoseRepairedRatio());
        vo.setStabilityScore(record.getStabilityScore());
        vo.setSymmetryScore(record.getSymmetryScore());
        vo.setPoseQuality(record.getPoseQuality());
        vo.setPoseEngineId(record.getPoseEngineId());
        vo.setRemark(record.getRemark());
        if (context.plan() != null) {
            vo.setPlanId(context.plan().getPlanId());
            vo.setPlanName(context.plan().getName());
            vo.setPatientId(context.plan().getPatientId());
            vo.setPatientName(userName(context.plan().getPatientId()));
        }
        if (context.actionName() != null) {
            vo.setActionName(context.actionName());
        }
        if (context.task() != null) {
            vo.setTaskDate(context.task().getTaskDate());
        }
        return vo;
    }

    @Override
    public PageResult<TherapistErrorLogVO> listErrors(Long recordId, Integer page, Integer size) {
        requireRecordAccess(recordId);
        Page<TrainingErrorLog> mpPage = new Page<>(page, size);
        trainingErrorLogMapper.selectPage(mpPage, new LambdaQueryWrapper<TrainingErrorLog>()
                .eq(TrainingErrorLog::getRecordId, recordId)
                .orderByAsc(TrainingErrorLog::getFrameNo)
                .orderByAsc(TrainingErrorLog::getErrorId));
        List<TherapistErrorLogVO> voList = mpPage.getRecords().stream().map(this::toErrorVO).toList();
        return PageResult.of(mpPage, voList);
    }

    @Override
    public PageResult<TherapistAnnotationVO> listAnnotations(Long recordId, Integer page, Integer size) {
        requireRecordAccess(recordId);
        Page<TrainingAnnotation> mpPage = new Page<>(page, size);
        trainingAnnotationMapper.selectPage(mpPage, new LambdaQueryWrapper<TrainingAnnotation>()
                .eq(TrainingAnnotation::getRecordId, recordId)
                .orderByDesc(TrainingAnnotation::getCreatedAt)
                .orderByDesc(TrainingAnnotation::getAnnotationId));
        Map<Long, String> names = userNames(mpPage.getRecords().stream()
                .map(TrainingAnnotation::getTherapistId).distinct().toList());
        List<TherapistAnnotationVO> voList = mpPage.getRecords().stream()
                .map(a -> toAnnotationVO(a, names.get(a.getTherapistId())))
                .toList();
        return PageResult.of(mpPage, voList);
    }

    @Override
    public TherapistAnnotationVO addAnnotation(Long recordId, String annotation) {
        requireRecordAccess(recordId);
        TrainingAnnotation entity = new TrainingAnnotation();
        entity.setRecordId(recordId);
        entity.setTherapistId(SecurityUtils.getUserId());
        entity.setAnnotation(annotation);
        trainingAnnotationMapper.insert(entity);
        return toAnnotationVO(entity, userName(SecurityUtils.getUserId()));
    }

    @Override
    public byte[] exportCsv(Long patientId, LocalDate from, LocalDate to) {
        requireActiveBinding(patientId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        List<Long> taskIds = listTaskIdsOfMyScope(patientId, null);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"record_id", "patient_id", "plan_name", "action_name", "task_date", "started_at",
                "total_reps", "correct_reps", "total_score", "avg_rom", "hold_ratio", "pose_quality",
                "valid_frame_ratio"});
        if (taskIds.isEmpty()) {
            return csvBytes(rows);
        }
        LocalDateTime fromTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toTime = to != null ? to.atTime(23, 59, 59) : null;
        List<TrainingRecord> records = trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                .in(TrainingRecord::getDailyTaskId, taskIds)
                .ge(fromTime != null, TrainingRecord::getStartedAt, fromTime)
                .le(toTime != null, TrainingRecord::getStartedAt, toTime)
                .orderByAsc(TrainingRecord::getStartedAt));
        // 批量解析关联名称
        List<TherapistTrainingRecordVO> voList = toRecordVOs(records);
        Map<Long, TherapistTrainingRecordVO> voByRecord = voList.stream()
                .collect(Collectors.toMap(TherapistTrainingRecordVO::getRecordId, Function.identity(), (a, b) -> a));

        List<String[]> dataRows = new ArrayList<>();
        for (TrainingRecord record : records) {
            TherapistTrainingRecordVO vo = voByRecord.get(record.getRecordId());
            dataRows.add(new String[]{
                    String.valueOf(record.getRecordId()),
                    vo != null && vo.getPatientId() != null ? String.valueOf(vo.getPatientId()) : "",
                    csv(vo != null ? vo.getPlanName() : null),
                    csv(vo != null ? vo.getActionName() : null),
                    vo != null && vo.getTaskDate() != null ? vo.getTaskDate().toString() : "",
                    record.getStartedAt() != null ? CSV_DATE_TIME.format(record.getStartedAt()) : "",
                    num(record.getTotalReps()),
                    num(record.getCorrectReps()),
                    num(record.getTotalScore()),
                    num(record.getAvgRom()),
                    num(record.getHoldRatio()),
                    record.getPoseQuality() != null ? record.getPoseQuality() : "",
                    num(record.getValidFrameRatio())
            });
        }
        List<String[]> all = new ArrayList<>();
        all.add(rows.get(0));
        all.addAll(dataRows);
        return csvBytes(all);
    }

    @Override
    public List<PatientTrendPointVO> getPatientTrends(Long patientId, LocalDate from, LocalDate to,
                                                      String granularity) {
        requireActiveBinding(patientId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        return patientProgressService.getTrendsForPatient(patientId, from, to, null, granularity);
    }

    @Override
    public PatientProgressVO getPatientCompletion(Long patientId, Long planId, LocalDate from, LocalDate to) {
        requireActiveBinding(patientId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        return patientProgressService.getCompletionForPatient(patientId, planId, from, to);
    }

    // ==================== 私有辅助 ====================

    /** 本人负责计划（可按患者/计划过滤）的任务ID集合；过滤 planStatus=ARCHIVED 不排除（历史数据仍可见）。 */
    private List<Long> listTaskIdsOfMyScope(Long patientId, Long planId) {
        Long me = SecurityUtils.getUserId();
        List<Long> planIds = rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                        .eq(RehabPlan::getTherapistId, me)
                        .eq(patientId != null, RehabPlan::getPatientId, patientId)
                        .eq(planId != null, RehabPlan::getPlanId, planId)
                        .select(RehabPlan::getPlanId))
                .stream().map(RehabPlan::getPlanId).toList();
        if (planIds.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                        .in(PlanItem::getPlanId, planIds).select(PlanItem::getPlanItemId))
                .stream().map(PlanItem::getPlanItemId).toList();
        if (itemIds.isEmpty()) {
            return List.of();
        }
        return planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                        .in(PlanDailyTask::getPlanItemId, itemIds).select(PlanDailyTask::getTaskId))
                .stream().map(PlanDailyTask::getTaskId).toList();
    }

    /** 记录访问校验：存在且属于本人负责计划，否则 404/403。 */
    private RecordContext requireRecordAccess(Long recordId) {
        TrainingRecord record = trainingRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "训练记录不存在");
        }
        RecordContext context = loadRecordContext(record);
        requireMyPlan(context.plan());
        return context;
    }

    private record RecordContext(PlanDailyTask task, PlanItem item, RehabPlan plan, String actionName) {
    }

    private RecordContext loadRecordContext(TrainingRecord record) {
        PlanDailyTask task = record.getDailyTaskId() != null
                ? planDailyTaskMapper.selectById(record.getDailyTaskId()) : null;
        PlanItem item = task != null ? planItemMapper.selectById(task.getPlanItemId()) : null;
        RehabPlan plan = item != null ? rehabPlanMapper.selectById(item.getPlanId()) : null;
        String actionName = resolveActionName(record.getActionVersionId());
        return new RecordContext(task, item, plan, actionName);
    }

    private void requireMyPlan(RehabPlan plan) {
        if (plan == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "训练记录不存在");
        }
        if (!plan.getTherapistId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(ResultCode.OWNERSHIP_DENIED, "该记录不属于您负责的患者");
        }
    }

    private String resolveActionName(Long actionVersionId) {
        if (actionVersionId == null) {
            return null;
        }
        ActionVersion version = actionVersionMapper.selectById(actionVersionId);
        if (version == null) {
            return null;
        }
        Action action = actionMapper.selectById(version.getActionId());
        return action != null ? action.getName() : null;
    }

    private List<TherapistTrainingRecordVO> toRecordVOs(List<TrainingRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, PlanDailyTask> taskMap = planDailyTaskMapper.selectBatchIds(
                        records.stream().map(TrainingRecord::getDailyTaskId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(PlanDailyTask::getTaskId, Function.identity()));
        Map<Long, PlanItem> itemMap = taskMap.isEmpty() ? Map.of()
                : planItemMapper.selectBatchIds(taskMap.values().stream()
                        .map(PlanDailyTask::getPlanItemId).distinct().toList())
                .stream().collect(Collectors.toMap(PlanItem::getPlanItemId, Function.identity()));
        Map<Long, RehabPlan> planMap = itemMap.isEmpty() ? Map.of()
                : rehabPlanMapper.selectBatchIds(itemMap.values().stream()
                        .map(PlanItem::getPlanId).distinct().toList())
                .stream().collect(Collectors.toMap(RehabPlan::getPlanId, Function.identity()));
        List<Long> versionIds = records.stream()
                .map(TrainingRecord::getActionVersionId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> actionNames = new LinkedHashMap<>();
        if (!versionIds.isEmpty()) {
            Map<Long, Long> actionIdByVersion = actionVersionMapper.selectBatchIds(versionIds).stream()
                    .collect(Collectors.toMap(ActionVersion::getActionVersionId, ActionVersion::getActionId));
            actionIdByVersion.forEach((versionId, actionId) -> {
                Action action = actionMapper.selectById(actionId);
                if (action != null) {
                    actionNames.put(versionId, action.getName());
                }
            });
        }
        Map<Long, String> patientNames = userNames(planMap.values().stream()
                .map(RehabPlan::getPatientId).distinct().toList());

        return records.stream().map(record -> {
            PlanDailyTask task = taskMap.get(record.getDailyTaskId());
            PlanItem item = task != null ? itemMap.get(task.getPlanItemId()) : null;
            RehabPlan plan = item != null ? planMap.get(item.getPlanId()) : null;
            TherapistTrainingRecordVO vo = new TherapistTrainingRecordVO();
            vo.setRecordId(record.getRecordId());
            vo.setStartedAt(record.getStartedAt());
            vo.setEndedAt(record.getEndedAt());
            vo.setTotalReps(record.getTotalReps());
            vo.setCorrectReps(record.getCorrectReps());
            vo.setTotalScore(record.getTotalScore());
            vo.setPoseQuality(record.getPoseQuality());
            vo.setValidFrameRatio(record.getValidFrameRatio());
            vo.setAvgRom(record.getAvgRom());
            vo.setHoldRatio(record.getHoldRatio());
            vo.setActionName(actionNames.get(record.getActionVersionId()));
            if (task != null) {
                vo.setTaskDate(task.getTaskDate());
            }
            if (plan != null) {
                vo.setPlanId(plan.getPlanId());
                vo.setPlanName(plan.getName());
                vo.setPatientId(plan.getPatientId());
                vo.setPatientName(patientNames.get(plan.getPatientId()));
            }
            return vo;
        }).toList();
    }

    private TherapistErrorLogVO toErrorVO(TrainingErrorLog error) {
        TherapistErrorLogVO vo = new TherapistErrorLogVO();
        vo.setErrorId(error.getErrorId());
        vo.setDimension(error.getDimension());
        vo.setPhase(error.getPhase());
        vo.setErrorType(error.getErrorType());
        vo.setJointName(error.getJointName());
        vo.setCurrentValue(error.getCurrentValue());
        vo.setExpectedValue(error.getExpectedValue());
        vo.setDeviation(error.getDeviation());
        vo.setSeverity(error.getSeverity());
        vo.setFrameNo(error.getFrameNo());
        vo.setPromptText(error.getPromptText());
        vo.setErrorAt(error.getErrorAt());
        return vo;
    }

    private TherapistAnnotationVO toAnnotationVO(TrainingAnnotation annotation, String therapistName) {
        TherapistAnnotationVO vo = new TherapistAnnotationVO();
        vo.setAnnotationId(annotation.getAnnotationId());
        vo.setRecordId(annotation.getRecordId());
        vo.setTherapistId(annotation.getTherapistId());
        vo.setTherapistName(therapistName);
        vo.setAnnotation(annotation.getAnnotation());
        vo.setCreatedAt(annotation.getCreatedAt());
        return vo;
    }

    private Map<String, BigDecimal> parseScoreDetail(String scoreDetailJson) {
        if (!StringUtils.hasText(scoreDetailJson)) {
            return Map.of();
        }
        try {
            Map<String, Object> raw = JSONUtil.toBean(scoreDetailJson, Map.class);
            Map<String, BigDecimal> result = new LinkedHashMap<>();
            raw.forEach((key, value) -> {
                if (key != null && value != null) {
                    result.put(key, new BigDecimal(String.valueOf(value)));
                }
            });
            return result;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** 校验当前治疗师与患者的 ACTIVE 绑定，解除/不存在一律 403。 */
    private void requireActiveBinding(Long patientId) {
        Long me = SecurityUtils.getUserId();
        PatientTherapist binding = patientTherapistMapper.selectOne(new LambdaQueryWrapper<PatientTherapist>()
                .eq(PatientTherapist::getTherapistId, me)
                .eq(PatientTherapist::getPatientId, patientId)
                .eq(PatientTherapist::getStatus, BindingStatus.ACTIVE.getCode())
                .last("limit 1"));
        if (binding == null) {
            throw new BusinessException(ResultCode.OWNERSHIP_DENIED, "该患者不在您的负责列表中或绑定已解除");
        }
    }

    private Map<Long, String> userNames(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return patientProfileMapper.selectList(new LambdaQueryWrapper<PatientProfile>()
                        .in(PatientProfile::getUserId, userIds)).stream()
                .filter(p -> StringUtils.hasText(p.getRealName()))
                .collect(Collectors.toMap(PatientProfile::getUserId, PatientProfile::getRealName));
    }

    private String userName(Long userId) {
        return userNames(List.of(userId)).getOrDefault(userId, null);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String num(BigDecimal value) {
        return value != null ? value.toPlainString() : "";
    }

    private String num(Integer value) {
        return value != null ? String.valueOf(value) : "";
    }

    /** 组装 CSV 字节（UTF-8 BOM 头，便于 Excel 识别中文）。 */
    private byte[] csvBytes(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            sb.append(String.join(",", row)).append("\r\n");
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);
        return result;
    }
}
