package com.rehab.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.PatientPlanService;
import com.rehab.app.vo.PatientPlanItemVO;
import com.rehab.app.vo.PatientPlanVO;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.entity.PlanItem;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.model.entity.RehabPlan;
import com.rehab.model.entity.TherapistProfile;
import com.rehab.model.enums.PlanStatus;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.model.mapper.account.TherapistProfileMapper;
import com.rehab.model.mapper.plan.PlanItemMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.system.RehabPartDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 患者端康复计划服务实现：仅查本人计划，越权一律 404。
 */
@Service
@RequiredArgsConstructor
public class PatientPlanServiceImpl implements PatientPlanService {

    private final RehabPlanMapper rehabPlanMapper;
    private final PlanItemMapper planItemMapper;
    private final ActionVersionMapper actionVersionMapper;
    private final ActionMapper actionMapper;
    private final TherapistProfileMapper therapistProfileMapper;
    private final RehabPartDictMapper rehabPartDictMapper;

    @Override
    public PageResult<PatientPlanVO> listMyPlans(Integer page, Integer size, String status) {
        Long userId = SecurityUtils.getUserId();
        if (StringUtils.hasText(status)) {
            validateStatus(status);
        }
        Page<RehabPlan> mpPage = new Page<>(page, size);
        rehabPlanMapper.selectPage(mpPage, new LambdaQueryWrapper<RehabPlan>()
                .eq(RehabPlan::getPatientId, userId)
                .eq(StringUtils.hasText(status), RehabPlan::getStatus, status)
                .orderByDesc(RehabPlan::getCreatedAt));
        List<PatientPlanVO> voList = mpPage.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(mpPage, voList);
    }

    @Override
    public PatientPlanVO getMyPlanDetail(Long planId) {
        RehabPlan plan = requireOwnPlan(planId);
        return toVO(plan);
    }

    @Override
    public List<PatientPlanItemVO> listMyPlanItems(Long planId) {
        requireOwnPlan(planId);
        List<PlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId)
                .orderByAsc(PlanItem::getSequenceNo));
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量取版本与动作名称，避免 N+1
        List<Long> versionIds = items.stream().map(PlanItem::getActionVersionId).distinct().collect(Collectors.toList());
        Map<Long, ActionVersion> versionMap = versionIds.isEmpty() ? Collections.emptyMap()
                : actionVersionMapper.selectBatchIds(versionIds).stream()
                        .collect(Collectors.toMap(ActionVersion::getActionVersionId, v -> v));
        List<Long> actionIds = versionMap.values().stream().map(ActionVersion::getActionId).distinct().collect(Collectors.toList());
        Map<Long, Action> actionMap = actionIds.isEmpty() ? Collections.emptyMap()
                : actionMapper.selectBatchIds(actionIds).stream()
                        .collect(Collectors.toMap(Action::getActionId, a -> a));

        return items.stream().map(item -> {
            PatientPlanItemVO vo = new PatientPlanItemVO();
            vo.setPlanItemId(item.getPlanItemId());
            vo.setPlanId(item.getPlanId());
            vo.setActionVersionId(item.getActionVersionId());
            vo.setSequenceNo(item.getSequenceNo());
            vo.setTargetSets(item.getTargetSets());
            vo.setTargetReps(item.getTargetReps());
            vo.setTargetAmplitude(item.getTargetAmplitude());
            vo.setDurationSec(item.getDurationSec());
            vo.setRestSec(item.getRestSec());
            vo.setInstruction(item.getInstruction());
            ActionVersion version = versionMap.get(item.getActionVersionId());
            if (version != null) {
                vo.setVersionNo(version.getVersionNo());
                vo.setActionId(version.getActionId());
                Action action = actionMap.get(version.getActionId());
                if (action != null) {
                    vo.setActionName(action.getName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 私有辅助 ====================

    /** 校验计划归属：不存在或不属于本人统一 404，避免泄露他人资源存在性。 */
    private RehabPlan requireOwnPlan(Long planId) {
        RehabPlan plan = rehabPlanMapper.selectById(planId);
        if (plan == null || !plan.getPatientId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "计划不存在");
        }
        return plan;
    }

    private void validateStatus(String status) {
        try {
            PlanStatus.fromCode(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.INVALID_ENUM, "非法的计划状态：" + status);
        }
    }

    private PatientPlanVO toVO(RehabPlan plan) {
        PatientPlanVO vo = new PatientPlanVO();
        vo.setPlanId(plan.getPlanId());
        vo.setName(plan.getName());
        vo.setRehabPartCode(plan.getRehabPartCode());
        vo.setVersionNo(plan.getVersionNo());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setFrequencyType(plan.getFrequencyType());
        vo.setFrequencyValue(plan.getFrequencyValue());
        vo.setStatus(plan.getStatus());
        vo.setStatusName(PlanStatus.fromCode(plan.getStatus()).getName());
        vo.setRemark(plan.getRemark());
        vo.setCreatedAt(plan.getCreatedAt());

        if (StringUtils.hasText(plan.getRehabPartCode())) {
            RehabPartDict dict = rehabPartDictMapper.selectById(plan.getRehabPartCode());
            vo.setRehabPartName(dict != null ? dict.getName() : null);
        }
        if (plan.getTherapistId() != null) {
            TherapistProfile tp = therapistProfileMapper.selectOne(new LambdaQueryWrapper<TherapistProfile>()
                    .eq(TherapistProfile::getUserId, plan.getTherapistId())
                    .last("limit 1"));
            vo.setTherapistName(tp != null ? tp.getRealName() : null);
        }
        return vo;
    }
}
