package com.rehab.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.admin.service.AdminActionRuleService;
import com.rehab.model.mapper.action.ActionRuleMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.ResultCode;
import com.rehab.model.dto.ActionRuleRequest;
import com.rehab.model.entity.ActionRule;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.enums.VersionStatus;
import com.rehab.admin.vo.AdminActionRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端 - 动作规则服务实现。
 */
@Service
@RequiredArgsConstructor
public class AdminActionRuleServiceImpl extends ServiceImpl<ActionRuleMapper, ActionRule>
        implements AdminActionRuleService {

    private final ActionVersionMapper actionVersionMapper;

    @Override
    public List<AdminActionRuleVO> listRules(Long versionId) {
        requireVersion(versionId);
        List<ActionRule> rules = this.list(
                new LambdaQueryWrapper<ActionRule>()
                        .eq(ActionRule::getActionVersionId, versionId)
                        .orderByAsc(ActionRule::getCreatedAt));
        return rules.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(Long versionId, ActionRuleRequest request) {
        ActionVersion version = requireDraftVersion(versionId);

        if (request.getSymmetryPairRuleId() != null) {
            requireRuleInSameVersion(versionId, request.getSymmetryPairRuleId());
        }

        ActionRule rule = new ActionRule();
        rule.setActionVersionId(versionId);
        applyRequest(rule, request);
        this.save(rule);
        return rule.getActionRuleId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(Long versionId, Long ruleId, ActionRuleRequest request) {
        requireDraftVersion(versionId);
        ActionRule rule = requireRuleInSameVersion(versionId, ruleId);

        if (request.getSymmetryPairRuleId() != null) {
            requireRuleInSameVersion(versionId, request.getSymmetryPairRuleId());
        }

        applyRequest(rule, request);
        this.updateById(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long versionId, Long ruleId) {
        requireDraftVersion(versionId);
        ActionRule rule = requireRuleInSameVersion(versionId, ruleId);
        this.removeById(rule.getActionRuleId());
    }

    /**
     * 仅校验版本存在（查看规则列表不限制版本状态）。
     */
    private ActionVersion requireVersion(Long versionId) {
        ActionVersion version = actionVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(ResultCode.VERSION_NOT_FOUND, "动作版本不存在：" + versionId);
        }
        return version;
    }

    private ActionVersion requireDraftVersion(Long versionId) {
        ActionVersion version = actionVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(ResultCode.VERSION_NOT_FOUND, "动作版本不存在：" + versionId);
        }
        if (!VersionStatus.DRAFT.getCode().equals(version.getStatus())) {
            throw new BusinessException(ResultCode.VERSION_IMMUTABLE,
                    "已发布版本不可编辑规则，当前状态：" + version.getStatus());
        }
        return version;
    }

    private ActionRule requireRuleInSameVersion(Long versionId, Long ruleId) {
        ActionRule rule = this.getById(ruleId);
        if (rule == null || !versionId.equals(rule.getActionVersionId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在或不属于当前版本：" + ruleId);
        }
        return rule;
    }

    private void applyRequest(ActionRule rule, ActionRuleRequest request) {
        rule.setJointName(request.getJointName());
        // 数据库列为 NOT NULL，空值统一回落
        rule.setKeypointIndices(StringUtils.hasText(request.getKeypointIndices()) ? request.getKeypointIndices() : "[]");
        rule.setAngleFormula(StringUtils.hasText(request.getAngleFormula()) ? request.getAngleFormula() : "");
        rule.setJointRole(request.getJointRole().getCode());
        rule.setDimension(request.getDimension().getCode());
        rule.setPhase(request.getPhase().getCode());
        rule.setMinAngle(request.getMinAngle());
        rule.setMaxAngle(request.getMaxAngle());
        rule.setStandardAngle(request.getStandardAngle());
        rule.setToleranceSoft(request.getToleranceSoft());
        rule.setToleranceHard(request.getToleranceHard());
        rule.setHoldFramesRequired(request.getHoldFramesRequired());
        rule.setSpeedMin(request.getSpeedMin());
        rule.setSpeedMax(request.getSpeedMax());
        rule.setScoreWeight(request.getScoreWeight() != null ? request.getScoreWeight() : BigDecimal.ZERO);
        rule.setSeverity(request.getSeverity() != null ? request.getSeverity() : 1);
        rule.setPromptText(StringUtils.hasText(request.getPromptText()) ? request.getPromptText() : "");
        rule.setSymmetryPairRuleId(request.getSymmetryPairRuleId());
    }

    private AdminActionRuleVO toVO(ActionRule r) {
        return AdminActionRuleVO.builder()
                .ruleId(r.getActionRuleId())
                .jointName(r.getJointName())
                .keypointIndices(r.getKeypointIndices())
                .angleFormula(r.getAngleFormula())
                .jointRole(r.getJointRole())
                .dimension(r.getDimension())
                .phase(r.getPhase())
                .minAngle(r.getMinAngle())
                .maxAngle(r.getMaxAngle())
                .standardAngle(r.getStandardAngle())
                .toleranceSoft(r.getToleranceSoft())
                .toleranceHard(r.getToleranceHard())
                .holdFramesRequired(r.getHoldFramesRequired())
                .speedMin(r.getSpeedMin())
                .speedMax(r.getSpeedMax())
                .scoreWeight(r.getScoreWeight())
                .severity(r.getSeverity())
                .promptText(r.getPromptText())
                .symmetryPairRuleId(r.getSymmetryPairRuleId())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
