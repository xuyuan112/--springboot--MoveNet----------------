package com.rehab.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.TherapistPatientService;
import com.rehab.app.vo.TherapistAlertVO;
import com.rehab.app.vo.TherapistPatientDetailVO;
import com.rehab.app.vo.TherapistPatientTaskVO;
import com.rehab.app.vo.TherapistPatientVO;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.entity.Consultation;
import com.rehab.model.entity.ConsultationMessage;
import com.rehab.model.entity.PatientProfile;
import com.rehab.model.entity.PatientRehabPart;
import com.rehab.model.entity.PatientTherapist;
import com.rehab.model.entity.PlanDailyTask;
import com.rehab.model.entity.PlanItem;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.TrainingRecord;
import com.rehab.model.entity.UserAccount;
import com.rehab.model.enums.BindingStatus;
import com.rehab.model.enums.DailyTaskStatus;
import com.rehab.model.enums.PatientPartStatus;
import com.rehab.model.enums.PlanStatus;
import com.rehab.model.mapper.account.PatientProfileMapper;
import com.rehab.model.mapper.account.PatientRehabPartMapper;
import com.rehab.model.mapper.account.PatientTherapistMapper;
import com.rehab.model.mapper.account.UserAccountMapper;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.model.mapper.consultation.ConsultationMapper;
import com.rehab.model.mapper.consultation.ConsultationMessageMapper;
import com.rehab.model.mapper.plan.PlanDailyTaskMapper;
import com.rehab.model.mapper.plan.PlanItemMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.system.RehabPartDictMapper;
import com.rehab.model.mapper.training.TrainingRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 治疗师端患者管理服务实现（P6-B2）。
 *
 * <p>首版量化口径（仅训练管理提示，不构成医学诊断）：患者列表内存过滤分页；
 * 异常提示近 7 日窗口：单次 total_score&lt;60、近 3 天有任务均未完成、
 * 训练备注/患者咨询消息提及疼痛；手机号脱敏（前 3 后 4）。</p>
 */
@Service
@RequiredArgsConstructor
public class TherapistPatientServiceImpl implements TherapistPatientService {

    /** 低分训练提示阈值 */
    private static final BigDecimal LOW_SCORE_THRESHOLD = new BigDecimal("60");

    /** 异常提示回看窗口（天） */
    private static final int ALERT_WINDOW_DAYS = 7;

    private static final String ALERT_LOW_SCORE = "LOW_SCORE";
    private static final String ALERT_NO_COMPLETION_3D = "NO_COMPLETION_3D";
    private static final String ALERT_PAIN_FEEDBACK = "PAIN_FEEDBACK";

    private final PatientTherapistMapper patientTherapistMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final UserAccountMapper userAccountMapper;
    private final PatientRehabPartMapper patientRehabPartMapper;
    private final RehabPartDictMapper rehabPartDictMapper;
    private final RehabPlanMapper rehabPlanMapper;
    private final PlanItemMapper planItemMapper;
    private final PlanDailyTaskMapper planDailyTaskMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final ConsultationMapper consultationMapper;
    private final ConsultationMessageMapper consultationMessageMapper;
    private final ActionVersionMapper actionVersionMapper;
    private final ActionMapper actionMapper;

    // ==================== 患者列表 / 详情 ====================

    @Override
    public PageResult<TherapistPatientVO> listPatients(Integer page, Integer size, String keyword,
                                                       String partCode, String planStatus) {
        Long me = SecurityUtils.getUserId();
        List<PatientTherapist> bindings = patientTherapistMapper.selectList(new LambdaQueryWrapper<PatientTherapist>()
                .eq(PatientTherapist::getTherapistId, me)
                .eq(PatientTherapist::getStatus, BindingStatus.ACTIVE.getCode())
                .orderByDesc(PatientTherapist::getStartAt)
                .orderByDesc(PatientTherapist::getBindingId));
        if (bindings.isEmpty()) {
            return PageResult.empty(Long.valueOf(page), Long.valueOf(size));
        }
        // 同一患者可能并存主治/协作多条 ACTIVE 记录，按最近绑定去重
        Map<Long, PatientTherapist> bindingMap = new LinkedHashMap<>();
        bindings.forEach(binding -> bindingMap.putIfAbsent(binding.getPatientId(), binding));
        List<Long> patientIds = new ArrayList<>(bindingMap.keySet());

        Map<Long, PatientProfile> profileMap = patientProfileMapper.selectList(new LambdaQueryWrapper<PatientProfile>()
                        .in(PatientProfile::getUserId, patientIds)).stream()
                .collect(Collectors.toMap(PatientProfile::getUserId, Function.identity(), (a, b) -> a));
        Map<Long, UserAccount> accountMap = userAccountMapper.selectBatchIds(patientIds).stream()
                .collect(Collectors.toMap(UserAccount::getUserId, Function.identity()));
        Map<Long, PatientRehabPart> primaryPartMap = patientRehabPartMapper.selectList(
                        new LambdaQueryWrapper<PatientRehabPart>()
                                .in(PatientRehabPart::getPatientId, patientIds)
                                .eq(PatientRehabPart::getPrimaryFlag, 1)
                                .eq(PatientRehabPart::getStatus, PatientPartStatus.ACTIVE.getCode()))
                .stream()
                .collect(Collectors.toMap(PatientRehabPart::getPatientId, Function.identity(), (a, b) -> a));
        // 本人创建的全部计划（planId 倒序，组内首条即“最新计划”）
        Map<Long, List<RehabPlan>> plansByPatient = rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                        .eq(RehabPlan::getTherapistId, me)
                        .in(RehabPlan::getPatientId, patientIds)
                        .orderByDesc(RehabPlan::getPlanId))
                .stream()
                .collect(Collectors.groupingBy(RehabPlan::getPatientId));

        Set<String> partCodes = new HashSet<>();
        primaryPartMap.values().forEach(part -> partCodes.add(part.getRehabPartCode()));
        plansByPatient.values().stream().flatMap(List::stream)
                .map(RehabPlan::getRehabPartCode)
                .filter(StringUtils::hasText)
                .forEach(partCodes::add);
        Map<String, RehabPartDict> dictMap = partCodes.isEmpty() ? Map.of()
                : rehabPartDictMapper.selectBatchIds(partCodes).stream()
                        .collect(Collectors.toMap(RehabPartDict::getCode, Function.identity()));

        // 轻量行构建 + 内存过滤（keyword 用脱敏前的原始手机号匹配）
        List<TherapistPatientVO> rows = new ArrayList<>();
        for (Long patientId : patientIds) {
            PatientProfile profile = profileMap.get(patientId);
            UserAccount account = accountMap.get(patientId);
            List<RehabPlan> patientPlans = plansByPatient.getOrDefault(patientId, List.of());
            RehabPlan latestPlan = patientPlans.isEmpty() ? null : patientPlans.get(0);

            if (StringUtils.hasText(keyword)) {
                String kw = keyword.trim();
                boolean hit = (profile != null && StringUtils.hasText(profile.getRealName())
                        && profile.getRealName().contains(kw))
                        || (account != null && StringUtils.hasText(account.getPhone())
                                && account.getPhone().contains(kw));
                if (!hit) {
                    continue;
                }
            }
            PatientRehabPart primaryPart = primaryPartMap.get(patientId);
            if (StringUtils.hasText(partCode)
                    && (primaryPart == null || !partCode.equals(primaryPart.getRehabPartCode()))) {
                continue;
            }
            if (StringUtils.hasText(planStatus)
                    && (latestPlan == null || !planStatus.equals(latestPlan.getStatus()))) {
                continue;
            }

            TherapistPatientVO vo = new TherapistPatientVO();
            vo.setPatientId(patientId);
            vo.setName(profile != null ? profile.getRealName() : null);
            vo.setGender(profile != null ? profile.getGender() : null);
            vo.setBirthday(profile != null ? profile.getBirthDate() : null);
            vo.setPhone(account != null ? maskPhone(account.getPhone()) : null);
            if (primaryPart != null && dictMap.get(primaryPart.getRehabPartCode()) != null) {
                vo.setRehabPartName(dictMap.get(primaryPart.getRehabPartCode()).getName());
            }
            PatientTherapist binding = bindingMap.get(patientId);
            vo.setRelationType(binding.getRelationType());
            vo.setBindingStartAt(binding.getStartAt());
            vo.setActivePlanCount(patientPlans.stream().filter(this::isLivePlan).count());
            if (latestPlan != null) {
                vo.setLatestPlanStatus(latestPlan.getStatus());
                vo.setLatestPlanStatusName(safePlanStatusName(latestPlan.getStatus()));
            }
            rows.add(vo);
        }

        long total = rows.size();
        int from = (int) Math.min((long) (page - 1) * size, total);
        int to = (int) Math.min(from + (long) size, total);
        List<TherapistPatientVO> pageRows = new ArrayList<>(rows.subList(from, to));
        fillPageMetrics(pageRows, plansByPatient);
        return new PageResult<>(pageRows, Long.valueOf(page), Long.valueOf(size), total);
    }

    @Override
    public TherapistPatientDetailVO getPatientDetail(Long patientId) {
        PatientTherapist binding = requireActiveBinding(patientId);
        PatientProfile profile = patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, patientId)
                .last("limit 1"));
        UserAccount account = userAccountMapper.selectById(patientId);
        PatientRehabPart primaryPart = patientRehabPartMapper.selectOne(new LambdaQueryWrapper<PatientRehabPart>()
                .eq(PatientRehabPart::getPatientId, patientId)
                .eq(PatientRehabPart::getPrimaryFlag, 1)
                .eq(PatientRehabPart::getStatus, PatientPartStatus.ACTIVE.getCode())
                .orderByDesc(PatientRehabPart::getPatientRehabPartId)
                .last("limit 1"));
        RehabPartDict dict = primaryPart != null
                ? rehabPartDictMapper.selectById(primaryPart.getRehabPartCode()) : null;
        List<RehabPlan> patientPlans = rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getTherapistId, SecurityUtils.getUserId())
                .eq(RehabPlan::getPatientId, patientId)
                .orderByDesc(RehabPlan::getPlanId));

        TherapistPatientDetailVO vo = new TherapistPatientDetailVO();
        vo.setPatientId(patientId);
        vo.setName(profile != null ? profile.getRealName() : null);
        vo.setGender(profile != null ? profile.getGender() : null);
        vo.setBirthday(profile != null ? profile.getBirthDate() : null);
        vo.setPhone(account != null ? maskPhone(account.getPhone()) : null);
        if (dict != null) {
            vo.setRehabPartName(dict.getName());
        }
        vo.setRelationType(binding.getRelationType());
        vo.setBindingStartAt(binding.getStartAt());
        vo.setActivePlanCount(patientPlans.stream().filter(this::isLivePlan).count());
        if (!patientPlans.isEmpty()) {
            RehabPlan latestPlan = patientPlans.get(0);
            vo.setLatestPlanStatus(latestPlan.getStatus());
            vo.setLatestPlanStatusName(safePlanStatusName(latestPlan.getStatus()));
        }
        if (profile != null) {
            vo.setHeightCm(profile.getHeightCm());
            vo.setWeightKg(profile.getWeightKg());
            vo.setMedicalHistory(profile.getMedicalHistory());
            vo.setAllergyInfo(profile.getAllergyInfo());
            vo.setContraindication(profile.getContraindication());
        }
        vo.setRiskHints(buildAlerts(patientId).stream().map(TherapistAlertVO::getContent).toList());
        return vo;
    }

    // ==================== 每日任务 ====================

    @Override
    public PageResult<TherapistPatientTaskVO> listPatientTasks(Long patientId, LocalDate from, LocalDate to,
                                                               String status, Integer page, Integer size) {
        requireActiveBinding(patientId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        List<RehabPlan> plans = rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getTherapistId, SecurityUtils.getUserId())
                .eq(RehabPlan::getPatientId, patientId));
        if (plans.isEmpty()) {
            return PageResult.empty(Long.valueOf(page), Long.valueOf(size));
        }
        Map<Long, String> planNameMap = plans.stream()
                .collect(Collectors.toMap(RehabPlan::getPlanId, RehabPlan::getName, (a, b) -> a));
        List<PlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .in(PlanItem::getPlanId, planNameMap.keySet()));
        if (items.isEmpty()) {
            return PageResult.empty(Long.valueOf(page), Long.valueOf(size));
        }
        Map<Long, PlanItem> itemMap = items.stream()
                .collect(Collectors.toMap(PlanItem::getPlanItemId, Function.identity()));

        Page<PlanDailyTask> mpPage = planDailyTaskMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PlanDailyTask>()
                        .in(PlanDailyTask::getPlanItemId, itemMap.keySet())
                        .ge(from != null, PlanDailyTask::getTaskDate, from)
                        .le(to != null, PlanDailyTask::getTaskDate, to)
                        .eq(StringUtils.hasText(status), PlanDailyTask::getStatus, status)
                        .orderByDesc(PlanDailyTask::getTaskDate)
                        .orderByDesc(PlanDailyTask::getTaskId));
        List<TherapistPatientTaskVO> voList = buildTaskVOs(mpPage.getRecords(), itemMap, planNameMap);
        return PageResult.of(mpPage, voList);
    }

    // ==================== 异常提示 ====================

    @Override
    public PageResult<TherapistAlertVO> listPatientAlerts(Long patientId, Integer page, Integer size) {
        requireActiveBinding(patientId);
        List<TherapistAlertVO> all = buildAlerts(patientId);
        int from = (int) Math.min((long) (page - 1) * size, all.size());
        int to = (int) Math.min(from + (long) size, all.size());
        return new PageResult<>(new ArrayList<>(all.subList(from, to)),
                Long.valueOf(page), Long.valueOf(size), (long) all.size());
    }

    /** 构建近 7 日异常提示全量列表（详情 riskHints 与分页接口共用）。 */
    private List<TherapistAlertVO> buildAlerts(Long patientId) {
        Long me = SecurityUtils.getUserId();
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(ALERT_WINDOW_DAYS - 1L);
        List<TherapistAlertVO> alerts = new ArrayList<>();

        List<RehabPlan> plans = rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getTherapistId, me)
                .eq(RehabPlan::getPatientId, patientId));
        if (!plans.isEmpty()) {
            Map<Long, String> planNameMap = plans.stream()
                    .collect(Collectors.toMap(RehabPlan::getPlanId, RehabPlan::getName, (a, b) -> a));
            List<PlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                    .in(PlanItem::getPlanId, planNameMap.keySet()));
            Map<Long, PlanItem> itemMap = items.stream()
                    .collect(Collectors.toMap(PlanItem::getPlanItemId, Function.identity(), (a, b) -> a));
            Map<Long, String> actionNameByItem = itemActionNames(items);
            List<PlanDailyTask> windowTasks = planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                    .in(PlanDailyTask::getPlanItemId, itemMap.keySet())
                    .ge(PlanDailyTask::getTaskDate, windowStart));
            if (!windowTasks.isEmpty()) {
                Map<Long, PlanDailyTask> taskById = windowTasks.stream()
                        .collect(Collectors.toMap(PlanDailyTask::getTaskId, Function.identity(), (a, b) -> a));
                List<TrainingRecord> records = trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                        .in(TrainingRecord::getDailyTaskId, taskById.keySet()));
                for (TrainingRecord record : records) {
                    PlanDailyTask task = taskById.get(record.getDailyTaskId());
                    if (task == null) {
                        continue;
                    }
                    String actionName = actionNameByItem.get(task.getPlanItemId());
                    if (record.getTotalScore() != null
                            && record.getTotalScore().compareTo(LOW_SCORE_THRESHOLD) < 0) {
                        alerts.add(buildAlert(ALERT_LOW_SCORE, "低分训练", task.getTaskDate(),
                                task.getTaskDate() + "「" + (StringUtils.hasText(actionName) ? actionName : "动作")
                                        + "」训练得分 " + record.getTotalScore().stripTrailingZeros().toPlainString()
                                        + " 分，低于 60 分提示阈值",
                                record.getRecordId()));
                    }
                    if (StringUtils.hasText(record.getRemark()) && record.getRemark().contains("疼")) {
                        alerts.add(buildAlert(ALERT_PAIN_FEEDBACK, "疼痛反馈", task.getTaskDate(),
                                task.getTaskDate() + "训练备注提及疼痛：" + truncate(record.getRemark()),
                                record.getRecordId()));
                    }
                }
                // 近 3 天（含今日）有任务但无一完成
                List<PlanDailyTask> recent3 = windowTasks.stream()
                        .filter(task -> !task.getTaskDate().isBefore(today.minusDays(2)))
                        .toList();
                boolean anyCompleted = recent3.stream()
                        .anyMatch(task -> DailyTaskStatus.COMPLETED.getCode().equals(task.getStatus()));
                if (!recent3.isEmpty() && !anyCompleted) {
                    alerts.add(buildAlert(ALERT_NO_COMPLETION_3D, "连续未完成", today,
                            "近 3 天共 " + recent3.size() + " 项任务均未完成，建议关注训练依从性", null));
                }
            }
        }

        // 咨询消息疼痛反馈（不限会话状态，近 7 日患者发送）
        List<Consultation> consultations = consultationMapper.selectList(new LambdaQueryWrapper<Consultation>()
                .eq(Consultation::getTherapistId, me)
                .eq(Consultation::getPatientId, patientId));
        if (!consultations.isEmpty()) {
            List<Long> consultationIds = consultations.stream()
                    .map(Consultation::getConsultationId).toList();
            List<ConsultationMessage> painMessages = consultationMessageMapper.selectList(
                    new LambdaQueryWrapper<ConsultationMessage>()
                            .in(ConsultationMessage::getConsultationId, consultationIds)
                            .eq(ConsultationMessage::getSenderId, patientId)
                            .ge(ConsultationMessage::getCreatedAt, windowStart.atStartOfDay())
                            .like(ConsultationMessage::getContent, "疼")
                            .orderByDesc(ConsultationMessage::getCreatedAt));
            for (ConsultationMessage message : painMessages) {
                alerts.add(buildAlert(ALERT_PAIN_FEEDBACK, "疼痛反馈", message.getCreatedAt().toLocalDate(),
                        message.getCreatedAt().toLocalDate() + "咨询消息提及疼痛：" + truncate(message.getContent()),
                        message.getConsultationId()));
            }
        }

        alerts.sort(Comparator.comparing(TherapistAlertVO::getAlertDate,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return alerts;
    }

    // ==================== 当页重指标 ====================

    /** 当页患者批量补齐：近 7 日任务完成率 + 低分风险标记（集合查询，避免逐行 N+1）。 */
    private void fillPageMetrics(List<TherapistPatientVO> rows, Map<Long, List<RehabPlan>> plansByPatient) {
        if (rows.isEmpty()) {
            return;
        }
        List<Long> pagePatientIds = rows.stream().map(TherapistPatientVO::getPatientId).toList();
        Map<Long, Long> planId2Patient = plansByPatient.entrySet().stream()
                .filter(entry -> pagePatientIds.contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toMap(RehabPlan::getPlanId, RehabPlan::getPatientId, (a, b) -> a));
        if (planId2Patient.isEmpty()) {
            return;
        }
        List<PlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .in(PlanItem::getPlanId, planId2Patient.keySet()));
        if (items.isEmpty()) {
            return;
        }
        Map<Long, Long> itemId2Patient = items.stream()
                .collect(Collectors.toMap(PlanItem::getPlanItemId,
                        item -> planId2Patient.get(item.getPlanId()), (a, b) -> a));
        LocalDate today = LocalDate.now();
        List<PlanDailyTask> windowTasks = planDailyTaskMapper.selectList(new LambdaQueryWrapper<PlanDailyTask>()
                .in(PlanDailyTask::getPlanItemId, itemId2Patient.keySet())
                .between(PlanDailyTask::getTaskDate, today.minusDays(6), today));
        if (windowTasks.isEmpty()) {
            return;
        }
        Map<Long, List<PlanDailyTask>> tasksByPatient = windowTasks.stream()
                .collect(Collectors.groupingBy(task -> itemId2Patient.get(task.getPlanItemId())));

        // 近 7 日完成率（一位小数；无任务保持 null）
        for (TherapistPatientVO vo : rows) {
            List<PlanDailyTask> tasks = tasksByPatient.get(vo.getPatientId());
            if (tasks == null || tasks.isEmpty()) {
                continue;
            }
            long completed = tasks.stream()
                    .filter(task -> DailyTaskStatus.COMPLETED.getCode().equals(task.getStatus()))
                    .count();
            vo.setRecentCompletionRate(Math.round(completed * 1000.0 / tasks.size()) / 10.0);
        }

        // 低分风险标记（近 7 日存在 total_score<60 的记录）
        List<Long> windowTaskIds = windowTasks.stream().map(PlanDailyTask::getTaskId).toList();
        Set<Long> lowTaskIds = trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                        .in(TrainingRecord::getDailyTaskId, windowTaskIds)
                        .lt(TrainingRecord::getTotalScore, LOW_SCORE_THRESHOLD))
                .stream()
                .map(TrainingRecord::getDailyTaskId)
                .collect(Collectors.toSet());
        if (lowTaskIds.isEmpty()) {
            return;
        }
        for (TherapistPatientVO vo : rows) {
            List<PlanDailyTask> tasks = tasksByPatient.get(vo.getPatientId());
            if (tasks != null && tasks.stream().anyMatch(task -> lowTaskIds.contains(task.getTaskId()))) {
                vo.setHasRiskHint(true);
            }
        }
    }

    // ==================== 私有辅助 ====================

    /** 校验当前治疗师与患者的 ACTIVE 绑定，解除/不存在一律 403。 */
    private PatientTherapist requireActiveBinding(Long patientId) {
        Long me = SecurityUtils.getUserId();
        PatientTherapist binding = patientTherapistMapper.selectOne(new LambdaQueryWrapper<PatientTherapist>()
                .eq(PatientTherapist::getTherapistId, me)
                .eq(PatientTherapist::getPatientId, patientId)
                .eq(PatientTherapist::getStatus, BindingStatus.ACTIVE.getCode())
                .last("limit 1"));
        if (binding == null) {
            throw new BusinessException(ResultCode.OWNERSHIP_DENIED, "该患者不在您的负责列表中或绑定已解除");
        }
        return binding;
    }

    private boolean isLivePlan(RehabPlan plan) {
        return PlanStatus.PUBLISHED.getCode().equals(plan.getStatus())
                || PlanStatus.PAUSED.getCode().equals(plan.getStatus());
    }

    private String safePlanStatusName(String status) {
        try {
            return PlanStatus.fromCode(status).getName();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }

    private String safeTaskStatusName(String status) {
        try {
            return DailyTaskStatus.fromCode(status).getName();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }

    /** 手机号脱敏：保留前 3 后 4。 */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.length() >= 8) {
            return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 4);
        }
        return "****";
    }

    /** 提示内容截断（避免长文本刷屏）。 */
    private String truncate(String text) {
        String trimmed = text.trim();
        return trimmed.length() > 50 ? trimmed.substring(0, 50) + "…" : trimmed;
    }

    private TherapistAlertVO buildAlert(String alertType, String alertTypeName, LocalDate alertDate,
                                        String content, Long refId) {
        TherapistAlertVO vo = new TherapistAlertVO();
        vo.setAlertType(alertType);
        vo.setAlertTypeName(alertTypeName);
        vo.setAlertDate(alertDate);
        vo.setContent(content);
        vo.setRefId(refId);
        return vo;
    }

    /** 计划项 → 动作名称映射（版本/动作两级批量查询）。 */
    private Map<Long, String> itemActionNames(List<PlanItem> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        List<Long> versionIds = items.stream().map(PlanItem::getActionVersionId).distinct().toList();
        Map<Long, Long> version2Action = actionVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(ActionVersion::getActionVersionId, ActionVersion::getActionId, (a, b) -> a));
        if (version2Action.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> actionNames = actionMapper
                .selectBatchIds(new ArrayList<>(new HashSet<>(version2Action.values()))).stream()
                .collect(Collectors.toMap(Action::getActionId, Action::getName, (a, b) -> a));
        Map<Long, String> result = new HashMap<>();
        for (PlanItem item : items) {
            Long actionId = version2Action.get(item.getActionVersionId());
            if (actionId != null) {
                result.put(item.getPlanItemId(), actionNames.get(actionId));
            }
        }
        return result;
    }

    /** 任务列表 VO 组装（动作名称两级批量查询，避免 N+1）。 */
    private List<TherapistPatientTaskVO> buildTaskVOs(List<PlanDailyTask> tasks,
                                                      Map<Long, PlanItem> itemMap,
                                                      Map<Long, String> planNameMap) {
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> versionIds = itemMap.values().stream()
                .map(PlanItem::getActionVersionId).distinct().toList();
        Map<Long, Long> version2Action = actionVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(ActionVersion::getActionVersionId, ActionVersion::getActionId, (a, b) -> a));
        Map<Long, String> actionNameMap = version2Action.isEmpty() ? Map.of()
                : actionMapper.selectBatchIds(new ArrayList<>(new HashSet<>(version2Action.values()))).stream()
                        .collect(Collectors.toMap(Action::getActionId, Action::getName, (a, b) -> a));
        return tasks.stream().map(task -> {
            PlanItem item = itemMap.get(task.getPlanItemId());
            TherapistPatientTaskVO vo = new TherapistPatientTaskVO();
            vo.setTaskId(task.getTaskId());
            vo.setTaskDate(task.getTaskDate());
            vo.setStatus(task.getStatus());
            vo.setStatusName(safeTaskStatusName(task.getStatus()));
            if (item != null) {
                vo.setPlanId(item.getPlanId());
                vo.setPlanName(planNameMap.get(item.getPlanId()));
                vo.setSequenceNo(item.getSequenceNo());
                Long actionId = version2Action.get(item.getActionVersionId());
                if (actionId != null) {
                    vo.setActionName(actionNameMap.get(actionId));
                }
            }
            vo.setTargetSets(task.getTargetSets());
            vo.setTargetReps(task.getTargetReps());
            return vo;
        }).collect(Collectors.toList());
    }
}
