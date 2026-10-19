package com.rehab.app.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.ReportGenerateService;
import com.rehab.app.service.TherapistReportService;
import com.rehab.app.vo.TherapistReportDetailVO;
import com.rehab.app.vo.TherapistReportVO;
import com.rehab.common.constant.Constants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.entity.AssessmentReport;
import com.rehab.model.entity.PatientProfile;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.UserAccount;
import com.rehab.model.enums.ReportStatus;
import com.rehab.model.dto.ReportReviewRequest;
import com.rehab.model.dto.ReportSubmitRequest;
import com.rehab.model.mapper.account.PatientProfileMapper;
import com.rehab.model.mapper.account.UserAccountMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.training.AssessmentReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 治疗师端评估报告服务实现（P7-B4）。
 *
 * <p>状态映射约定与患者端一致：接口文档 EFFECTIVE ↔ 枚举 PUBLISHED；
 * 提交（submit）→ REVIEWED；审核 APPROVE → PUBLISHED（患者端可见）、REJECT → DRAFT。
 * 报告归属经 plan.therapist_id=当前治疗师校验；AI 摘要仅 ai_draft 草稿字段。</p>
 */
@Service
@RequiredArgsConstructor
public class TherapistReportServiceImpl implements TherapistReportService {

    private final AssessmentReportMapper assessmentReportMapper;
    private final RehabPlanMapper rehabPlanMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final UserAccountMapper userAccountMapper;
    private final ReportGenerateService reportGenerateService;

    @Override
    public PageResult<TherapistReportVO> listReports(Integer page, Integer size, Long patientId, Long planId,
                                                     String status) {
        List<Long> planIds = myPlanIds(patientId, planId);
        if (planIds.isEmpty()) {
            return PageResult.empty(page.longValue(), size.longValue());
        }
        Page<AssessmentReport> mpPage = new Page<>(page, size);
        assessmentReportMapper.selectPage(mpPage, new LambdaQueryWrapper<AssessmentReport>()
                .in(AssessmentReport::getPlanId, planIds)
                .eq(StringUtils.hasText(status), AssessmentReport::getStatus, status)
                .orderByDesc(AssessmentReport::getPeriodStart)
                .orderByDesc(AssessmentReport::getVersionNo)
                .orderByDesc(AssessmentReport::getReportId));
        List<TherapistReportVO> voList = toReportVOs(mpPage.getRecords());
        return PageResult.of(mpPage, voList);
    }

    @Override
    public TherapistReportDetailVO getReportDetail(Long reportId) {
        ReportContext context = requireMyReport(reportId);
        return toDetailVO(context.report(), context.plan());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TherapistReportDetailVO submit(Long reportId, ReportSubmitRequest request) {
        ReportContext context = requireMyReport(reportId);
        AssessmentReport report = context.report();
        String status = report.getStatus();
        if (ReportStatus.PUBLISHED.getCode().equals(status)) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "报告已发布，不可修改结论");
        }
        report.setTherapistConclusion(request.getTherapistConclusion());
        report.setStatus(ReportStatus.REVIEWED.getCode());
        report.setReviewedBy(SecurityUtils.getUserId());
        report.setReviewedAt(LocalDateTime.now());
        assessmentReportMapper.updateById(report);
        return toDetailVO(report, context.plan());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TherapistReportDetailVO review(Long reportId, ReportReviewRequest request) {
        ReportContext context = requireMyReport(reportId);
        AssessmentReport report = context.report();
        boolean approve = "APPROVE".equalsIgnoreCase(request.getDecision());
        boolean reject = "REJECT".equalsIgnoreCase(request.getDecision());
        if (!approve && !reject) {
            throw new BusinessException(ResultCode.INVALID_ENUM, "审核决定仅支持 APPROVE/REJECT");
        }
        if (reject && !StringUtils.hasText(request.getComment())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "驳回时必须填写审核意见");
        }
        if (!ReportStatus.REVIEWED.getCode().equals(report.getStatus())) {
            throw new BusinessException(ResultCode.REPORT_NOT_REVIEWABLE,
                    "报告当前状态为 " + report.getStatus() + "，仅已提交（REVIEWED）报告可审核");
        }
        if (approve) {
            report.setStatus(ReportStatus.PUBLISHED.getCode());
        } else {
            report.setStatus(ReportStatus.DRAFT.getCode());
        }
        report.setReviewedBy(SecurityUtils.getUserId());
        report.setReviewedAt(LocalDateTime.now());
        assessmentReportMapper.updateById(report);
        return toDetailVO(report, context.plan());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TherapistReportDetailVO regenerate(Long reportId) {
        ReportContext context = requireMyReport(reportId);
        AssessmentReport origin = context.report();
        Long newReportId = reportGenerateService.regenerateForPlan(
                origin.getPlanId(), origin.getPeriodStart(), origin.getPeriodEnd());
        if (newReportId == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "报告重生成失败");
        }
        AssessmentReport created = assessmentReportMapper.selectById(newReportId);
        return toDetailVO(created, context.plan());
    }

    @Override
    public PageResult<TherapistReportVO> listVersions(Long reportId, Integer page, Integer size) {
        ReportContext context = requireMyReport(reportId);
        AssessmentReport origin = context.report();
        Page<AssessmentReport> mpPage = new Page<>(page, size);
        assessmentReportMapper.selectPage(mpPage, new LambdaQueryWrapper<AssessmentReport>()
                .eq(AssessmentReport::getPlanId, origin.getPlanId())
                .eq(AssessmentReport::getPeriodStart, origin.getPeriodStart())
                .eq(AssessmentReport::getPeriodEnd, origin.getPeriodEnd())
                .orderByDesc(AssessmentReport::getVersionNo)
                .orderByDesc(AssessmentReport::getReportId));
        List<TherapistReportVO> voList = toReportVOs(mpPage.getRecords());
        return PageResult.of(mpPage, voList);
    }

    // ==================== 私有辅助 ====================

    private record ReportContext(AssessmentReport report, RehabPlan plan) {
    }

    private ReportContext requireMyReport(Long reportId) {
        AssessmentReport report = assessmentReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报告不存在");
        }
        RehabPlan plan = rehabPlanMapper.selectById(report.getPlanId());
        if (plan == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报告不存在");
        }
        if (!plan.getTherapistId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(ResultCode.OWNERSHIP_DENIED, "该报告不属于您负责的患者");
        }
        return new ReportContext(report, plan);
    }

    private List<Long> myPlanIds(Long patientId, Long planId) {
        Long me = SecurityUtils.getUserId();
        return rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                        .eq(RehabPlan::getTherapistId, me)
                        .eq(patientId != null, RehabPlan::getPatientId, patientId)
                        .eq(planId != null, RehabPlan::getPlanId, planId)
                        .select(RehabPlan::getPlanId))
                .stream().map(RehabPlan::getPlanId).toList();
    }

    private List<TherapistReportVO> toReportVOs(List<AssessmentReport> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }
        Map<Long, RehabPlan> planMap = rehabPlanMapper.selectBatchIds(
                        reports.stream().map(AssessmentReport::getPlanId).distinct().toList()).stream()
                .collect(Collectors.toMap(RehabPlan::getPlanId, Function.identity()));
        Map<Long, String> patientNames = patientNames(planMap.values().stream()
                .map(RehabPlan::getPatientId).distinct().toList());
        return reports.stream().map(report -> {
            RehabPlan plan = planMap.get(report.getPlanId());
            TherapistReportVO vo = new TherapistReportVO();
            vo.setReportId(report.getReportId());
            vo.setPlanId(report.getPlanId());
            vo.setPlanName(plan != null ? plan.getName() : null);
            if (plan != null) {
                vo.setPatientId(plan.getPatientId());
                vo.setPatientName(patientNames.get(plan.getPatientId()));
            }
            vo.setPeriodStart(report.getPeriodStart());
            vo.setPeriodEnd(report.getPeriodEnd());
            vo.setVersionNo(report.getVersionNo());
            vo.setStatus(report.getStatus());
            vo.setReviewedAt(report.getReviewedAt());
            vo.setCreatedAt(report.getCreatedAt());
            return vo;
        }).toList();
    }

    private TherapistReportDetailVO toDetailVO(AssessmentReport report, RehabPlan plan) {
        TherapistReportDetailVO vo = new TherapistReportDetailVO();
        vo.setReportId(report.getReportId());
        vo.setPlanId(report.getPlanId());
        vo.setPlanName(plan != null ? plan.getName() : null);
        if (plan != null) {
            vo.setPatientId(plan.getPatientId());
            vo.setPatientName(patientNames(List.of(plan.getPatientId())).get(plan.getPatientId()));
        }
        vo.setPeriodStart(report.getPeriodStart());
        vo.setPeriodEnd(report.getPeriodEnd());
        vo.setVersionNo(report.getVersionNo());
        vo.setStatus(report.getStatus());
        vo.setDataSnapshot(parseSnapshot(report.getDataSnapshot()));
        vo.setAiDraft(report.getAiDraft());
        vo.setTherapistConclusion(report.getTherapistConclusion());
        if (report.getReviewedBy() != null) {
            vo.setReviewerName(patientNames(List.of(report.getReviewedBy()))
                    .getOrDefault(report.getReviewedBy(), null));
        }
        vo.setReviewedAt(report.getReviewedAt());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setDisclaimer(Constants.MEDICAL_DISCLAIMER);
        return vo;
    }

    /** 展示名解析：患者取 real_name，查不到（如治疗师审核人）回退账号表 username。 */
    private Map<Long, String> patientNames(List<Long> userIds) {
        Map<Long, String> result = new LinkedHashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        patientProfileMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PatientProfile>()
                        .in(PatientProfile::getUserId, userIds))
                .forEach(p -> {
                    if (StringUtils.hasText(p.getRealName())) {
                        result.put(p.getUserId(), p.getRealName());
                    }
                });
        List<Long> missing = userIds.stream().filter(id -> !result.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            userAccountMapper.selectBatchIds(missing).stream()
                    .filter(a -> StringUtils.hasText(a.getUsername()))
                    .forEach(a -> result.put(a.getUserId(), a.getUsername()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSnapshot(String snapshotJson) {
        if (!StringUtils.hasText(snapshotJson)) {
            return Map.of();
        }
        try {
            return JSONUtil.toBean(snapshotJson, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
