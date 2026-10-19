package com.rehab.app.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.PatientReportService;
import com.rehab.app.vo.PatientReportDetailVO;
import com.rehab.app.vo.PatientReportVO;
import com.rehab.common.constant.Constants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.entity.AssessmentReport;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.enums.ReportStatus;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.training.AssessmentReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 患者评估报告服务实现（P5-B2）。
 *
 * <p>状态映射约定：ReportStatus 枚举（DRAFT/AI_GENERATED/REVIEWED/PUBLISHED）为本仓唯一事实源，
 * 接口文档中的 EFFECTIVE 对应 PUBLISHED——患者列表与详情均只放行 PUBLISHED，
 * 其余状态（含不存在）统一 404，不泄露未审核内容。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientReportServiceImpl implements PatientReportService {

    private final AssessmentReportMapper assessmentReportMapper;
    private final RehabPlanMapper rehabPlanMapper;

    @Override
    public PageResult<PatientReportVO> pageMyReports(Integer page, Integer size, String status) {
        if (StringUtils.hasText(status) && !"EFFECTIVE".equalsIgnoreCase(status.trim())) {
            throw new BusinessException(ResultCode.INVALID_ENUM, "报告状态仅支持 EFFECTIVE");
        }
        Long userId = SecurityUtils.getUserId();
        Map<Long, RehabPlan> planMap = myPlanMap(userId);
        if (planMap.isEmpty()) {
            return PageResult.empty(page.longValue(), size.longValue());
        }
        Page<AssessmentReport> mpPage = new Page<>(page, size);
        assessmentReportMapper.selectPage(mpPage, new LambdaQueryWrapper<AssessmentReport>()
                .in(AssessmentReport::getPlanId, planMap.keySet())
                .eq(AssessmentReport::getStatus, ReportStatus.PUBLISHED.getCode())
                .orderByDesc(AssessmentReport::getPeriodStart)
                .orderByDesc(AssessmentReport::getReportId));
        List<PatientReportVO> voList = mpPage.getRecords().stream()
                .map(report -> toVO(report, planMap.get(report.getPlanId())))
                .toList();
        return PageResult.of(mpPage, voList);
    }

    @Override
    public PatientReportDetailVO getMyReport(Long reportId) {
        AssessmentReport report = assessmentReportMapper.selectById(reportId);
        // 非 PUBLISHED（含草稿/待审/已审未发布）统一 404，避免泄露未审核内容
        if (report == null || !ReportStatus.PUBLISHED.getCode().equals(report.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报告不存在或暂未开放");
        }
        Long userId = SecurityUtils.getUserId();
        RehabPlan plan = rehabPlanMapper.selectById(report.getPlanId());
        if (plan == null || !plan.getPatientId().equals(userId)) {
            // 他人报告与不存在同样返回 404，不泄露存在性
            throw new BusinessException(ResultCode.NOT_FOUND, "报告不存在或暂未开放");
        }
        PatientReportDetailVO vo = new PatientReportDetailVO();
        copyBase(report, plan, vo);
        applySnapshot(vo, report.getDataSnapshot());
        vo.setTherapistConclusion(report.getTherapistConclusion());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setDisclaimer(Constants.MEDICAL_DISCLAIMER);
        return vo;
    }

    // ==================== 私有辅助 ====================

    private Map<Long, RehabPlan> myPlanMap(Long userId) {
        List<RehabPlan> plans = rehabPlanMapper.selectList(new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getPatientId, userId));
        return plans.stream().collect(Collectors.toMap(RehabPlan::getPlanId, Function.identity()));
    }

    private PatientReportVO toVO(AssessmentReport report, RehabPlan plan) {
        PatientReportVO vo = new PatientReportVO();
        copyBase(report, plan, vo);
        return vo;
    }

    private void copyBase(AssessmentReport report, RehabPlan plan, PatientReportVO vo) {
        vo.setReportId(report.getReportId());
        vo.setPlanId(report.getPlanId());
        if (plan != null) {
            vo.setPlanName(plan.getName());
            vo.setPlanVersion(plan.getVersionNo());
        }
        vo.setPeriodStart(report.getPeriodStart());
        vo.setPeriodEnd(report.getPeriodEnd());
        vo.setStatus(report.getStatus());
        try {
            vo.setStatusName(ReportStatus.fromCode(report.getStatus()).getName());
        } catch (IllegalArgumentException e) {
            vo.setStatusName(report.getStatus());
        }
        vo.setCreatedAt(report.getCreatedAt());
    }

    /** 解析 data_snapshot（JSON 列），解析失败按空统计展示，不阻塞详情。 */
    private void applySnapshot(PatientReportDetailVO vo, String dataSnapshot) {
        if (!StringUtils.hasText(dataSnapshot)) {
            vo.setTotalTasks(0);
            vo.setCompletedTasks(0);
            vo.setCompletionRate(BigDecimal.ZERO);
            vo.setRecordCount(0);
            vo.setInsufficientCount(0);
            vo.setTotalReps(0);
            vo.setCorrectReps(0);
            vo.setScoreAverages(Collections.emptyMap());
            vo.setQualityDistribution(Collections.emptyMap());
            vo.setErrorDimensionDistribution(Collections.emptyMap());
            return;
        }
        try {
            JSONObject snap = JSONUtil.parseObj(dataSnapshot);
            vo.setTotalTasks(snap.getInt("totalTasks", 0));
            vo.setCompletedTasks(snap.getInt("completedTasks", 0));
            vo.setCompletionRate(snap.getBigDecimal("completionRate"));
            vo.setRecordCount(snap.getInt("recordCount", 0));
            vo.setInsufficientCount(snap.getInt("insufficientCount", 0));
            vo.setTotalReps(snap.getInt("totalReps", 0));
            vo.setCorrectReps(snap.getInt("correctReps", 0));
            vo.setAvgTotalScore(snap.getBigDecimal("avgTotalScore"));
            vo.setScoreAverages(toBigDecimalMap(snap.getJSONObject("scoreAverages")));
            vo.setQualityDistribution(toLongMap(snap.getJSONObject("qualityDistribution")));
            vo.setErrorDimensionDistribution(toLongMap(snap.getJSONObject("errorDimensionDistribution")));
        } catch (Exception e) {
            log.warn("报告 data_snapshot 解析失败 reportId={}：{}", vo.getReportId(), e.getMessage());
            vo.setScoreAverages(Collections.emptyMap());
            vo.setQualityDistribution(Collections.emptyMap());
            vo.setErrorDimensionDistribution(Collections.emptyMap());
        }
    }

    private Map<String, BigDecimal> toBigDecimalMap(JSONObject obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String key : obj.keySet()) {
            BigDecimal value = obj.getBigDecimal(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    private Map<String, Long> toLongMap(JSONObject obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new LinkedHashMap<>();
        for (String key : obj.keySet()) {
            Long value = obj.getLong(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }
}
