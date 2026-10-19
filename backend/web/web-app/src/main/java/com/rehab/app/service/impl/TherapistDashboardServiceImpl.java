package com.rehab.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.app.service.TherapistDashboardService;
import com.rehab.app.vo.TherapistDashboardVO;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.entity.AssessmentReport;
import com.rehab.model.entity.Consultation;
import com.rehab.model.entity.ConsultationMessage;
import com.rehab.model.entity.PatientTherapist;
import com.rehab.model.entity.PlanDailyTask;
import com.rehab.model.entity.PlanItem;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.TrainingRecord;
import com.rehab.model.enums.BindingStatus;
import com.rehab.model.enums.ConsultationStatus;
import com.rehab.model.enums.DailyTaskStatus;
import com.rehab.model.enums.ReportStatus;
import com.rehab.model.mapper.account.PatientTherapistMapper;
import com.rehab.model.mapper.consultation.ConsultationMapper;
import com.rehab.model.mapper.consultation.ConsultationMessageMapper;
import com.rehab.model.mapper.plan.PlanDailyTaskMapper;
import com.rehab.model.mapper.plan.PlanItemMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.training.AssessmentReportMapper;
import com.rehab.model.mapper.training.TrainingRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 治疗师工作台聚合服务实现（P6-B1）。
 *
 * <p>口径：负责患者数=ACTIVE 绑定去重；今日任务/完成率=本人计划当日生成的
 * plan_daily_task；待审核报告=本人计划下未生效报告（DRAFT/AI_GENERATED/REVIEWED）；
 * 待回复咨询=OPEN 会话中存在患者发送且未读消息；低分训练=当日本人计划任务下
 * total_score&lt;60 的训练记录数（仅提示，不构成医学判断）。</p>
 */
@Service
@RequiredArgsConstructor
public class TherapistDashboardServiceImpl implements TherapistDashboardService {

    /** 低分训练提示阈值（与患者列表风险口径一致） */
    private static final BigDecimal LOW_SCORE_THRESHOLD = new BigDecimal("60");

    private final PatientTherapistMapper patientTherapistMapper;
    private final RehabPlanMapper rehabPlanMapper;
    private final PlanItemMapper planItemMapper;
    private final PlanDailyTaskMapper planDailyTaskMapper;
    private final AssessmentReportMapper assessmentReportMapper;
    private final ConsultationMapper consultationMapper;
    private final ConsultationMessageMapper consultationMessageMapper;
    private final TrainingRecordMapper trainingRecordMapper;

    @Override
    public TherapistDashboardVO getDashboard() {
        Long me = SecurityUtils.getUserId();
        TherapistDashboardVO vo = new TherapistDashboardVO();

        // 1. 负责患者数（ACTIVE 绑定去重）
        List<PatientTherapist> bindings = patientTherapistMapper.selectList(
                new LambdaQueryWrapper<PatientTherapist>()
                        .eq(PatientTherapist::getTherapistId, me)
                        .eq(PatientTherapist::getStatus, BindingStatus.ACTIVE.getCode()));
        vo.setPatientCount(bindings.stream().map(PatientTherapist::getPatientId).distinct().count());

        // 2. 今日任务完成率（本人创建计划当日生成的任务）
        List<Long> myPlanIds = rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                        .eq(RehabPlan::getTherapistId, me)
                        .select(RehabPlan::getPlanId))
                .stream().map(RehabPlan::getPlanId).toList();
        List<Long> myItemIds = myPlanIds.isEmpty() ? Collections.emptyList()
                : planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                        .in(PlanItem::getPlanId, myPlanIds)
                        .select(PlanItem::getPlanItemId))
                .stream().map(PlanItem::getPlanItemId).toList();
        if (!myItemIds.isEmpty()) {
            List<PlanDailyTask> todayTasks = planDailyTaskMapper.selectList(
                    new LambdaQueryWrapper<PlanDailyTask>()
                            .in(PlanDailyTask::getPlanItemId, myItemIds)
                            .eq(PlanDailyTask::getTaskDate, LocalDate.now()));
            vo.setTodayTaskTotal((long) todayTasks.size());
            vo.setTodayTaskCompleted(todayTasks.stream()
                    .filter(task -> DailyTaskStatus.COMPLETED.getCode().equals(task.getStatus()))
                    .count());
            if (!todayTasks.isEmpty()) {
                vo.setTodayCompletionRate(Math.round(
                        vo.getTodayTaskCompleted() * 1000.0 / todayTasks.size()) / 10.0);
            }

            // 5. 今日低分训练（本人计划任务下 total_score<60）
            List<Long> todayTaskIds = todayTasks.stream().map(PlanDailyTask::getTaskId).toList();
            if (!todayTaskIds.isEmpty()) {
                vo.setLowScoreTodayCount(trainingRecordMapper.selectCount(
                        new LambdaQueryWrapper<TrainingRecord>()
                                .in(TrainingRecord::getDailyTaskId, todayTaskIds)
                                .lt(TrainingRecord::getTotalScore, LOW_SCORE_THRESHOLD)));
            }
        } else {
            vo.setTodayTaskTotal(0L);
            vo.setTodayTaskCompleted(0L);
        }

        // 3. 待审核报告数（本人计划下未生效报告）
        if (!myPlanIds.isEmpty()) {
            vo.setPendingReportCount(assessmentReportMapper.selectCount(
                    new LambdaQueryWrapper<AssessmentReport>()
                            .in(AssessmentReport::getPlanId, myPlanIds)
                            .in(AssessmentReport::getStatus, List.of(
                                    ReportStatus.DRAFT.getCode(),
                                    ReportStatus.AI_GENERATED.getCode(),
                                    ReportStatus.REVIEWED.getCode()))));
        } else {
            vo.setPendingReportCount(0L);
        }

        // 4. 待回复咨询数（OPEN 会话中存在患者发送且未读消息）
        List<Consultation> openConsultations = consultationMapper.selectList(
                new LambdaQueryWrapper<Consultation>()
                        .eq(Consultation::getTherapistId, me)
                        .eq(Consultation::getStatus, ConsultationStatus.OPEN.getCode()));
        if (!openConsultations.isEmpty()) {
            List<Long> consultationIds = openConsultations.stream()
                    .map(Consultation::getConsultationId).toList();
            var unreadByConsultation = consultationMessageMapper.selectList(
                            new LambdaQueryWrapper<ConsultationMessage>()
                                    .in(ConsultationMessage::getConsultationId, consultationIds)
                                    .eq(ConsultationMessage::getIsRead, 0))
                    .stream()
                    .filter(message -> openConsultations.stream()
                            .anyMatch(consultation -> consultation.getConsultationId()
                                    .equals(message.getConsultationId())
                                    && consultation.getPatientId().equals(message.getSenderId())))
                    .map(ConsultationMessage::getConsultationId)
                    .distinct()
                    .count();
            vo.setPendingConsultCount(unreadByConsultation);
        } else {
            vo.setPendingConsultCount(0L);
        }

        return vo;
    }
}
