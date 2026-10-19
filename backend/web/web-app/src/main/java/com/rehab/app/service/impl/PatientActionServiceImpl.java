package com.rehab.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.PatientActionService;
import com.rehab.app.vo.ActionRuleVO;
import com.rehab.app.vo.PatientActionDetailVO;
import com.rehab.app.vo.PatientActionVO;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.ActionRule;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.model.enums.ActionStatus;
import com.rehab.model.enums.Difficulty;
import com.rehab.model.enums.VersionStatus;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.action.ActionRuleMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.model.mapper.system.RehabPartDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 患者端动作库服务实现（P4-B1）。
 *
 * <p>仅展示 PUBLISHED 动作 + PUBLISHED 版本；动作下架（DISABLED）后从列表消失，
 * 但历史计划与训练记录仍可按版本 ID 关联查询（只读归档）。详情为版本快照只读下发。</p>
 */
@Service
@RequiredArgsConstructor
public class PatientActionServiceImpl implements PatientActionService {

    private final ActionMapper actionMapper;
    private final ActionVersionMapper actionVersionMapper;
    private final ActionRuleMapper actionRuleMapper;
    private final RehabPartDictMapper rehabPartDictMapper;

    @Override
    public PageResult<PatientActionVO> listActions(Integer page, Integer size, String partCode,
                                                   String difficulty, String keyword) {
        if (StringUtils.hasText(difficulty)) {
            try {
                Difficulty.fromCode(difficulty);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ResultCode.INVALID_ENUM, "非法的动作难度：" + difficulty);
            }
        }
        // 全部 PUBLISHED 版本（每动作取版本号最大的一条发布版本）
        List<ActionVersion> versions = actionVersionMapper.selectList(new LambdaQueryWrapper<ActionVersion>()
                .eq(ActionVersion::getStatus, VersionStatus.PUBLISHED.getCode())
                .orderByAsc(ActionVersion::getActionId)
                .orderByDesc(ActionVersion::getVersionNo));
        if (versions.isEmpty()) {
            return PageResult.empty(page.longValue(), size.longValue());
        }
        Map<Long, ActionVersion> latestPublishedByAction = versions.stream()
                .collect(Collectors.toMap(ActionVersion::getActionId, Function.identity(),
                        (a, b) -> a.getVersionNo() != null && b.getVersionNo() != null && a.getVersionNo() >= b.getVersionNo() ? a : b));

        Page<Action> mpPage = new Page<>(page, size);
        actionMapper.selectPage(mpPage, new LambdaQueryWrapper<Action>()
                .eq(Action::getStatus, ActionStatus.PUBLISHED.getCode())
                .eq(StringUtils.hasText(partCode), Action::getTargetPartCode, partCode)
                .eq(StringUtils.hasText(difficulty), Action::getDifficulty, difficulty)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Action::getName, keyword.trim())
                        .or()
                        .like(Action::getDescription, keyword.trim()))
                .orderByDesc(Action::getUpdatedAt));

        List<PatientActionVO> voList = mpPage.getRecords().stream()
                .filter(action -> latestPublishedByAction.containsKey(action.getActionId()))
                .map(action -> toVO(action, latestPublishedByAction.get(action.getActionId())))
                .collect(Collectors.toList());
        return PageResult.of(mpPage, voList);
    }

    @Override
    public PatientActionDetailVO getActionDetail(Long actionVersionId) {
        ActionVersion version = actionVersionMapper.selectById(actionVersionId);
        // 仅 PUBLISHED 版本对患者可见（历史计划/记录归档查询走任务与记录详情链路）
        if (version == null || !VersionStatus.PUBLISHED.getCode().equals(version.getStatus())) {
            throw new BusinessException(ResultCode.VERSION_NOT_FOUND, "动作版本不存在或已下架");
        }
        Action action = actionMapper.selectById(version.getActionId());
        if (action == null || !ActionStatus.PUBLISHED.getCode().equals(action.getStatus())) {
            throw new BusinessException(ResultCode.VERSION_NOT_FOUND, "动作不存在或已下架");
        }
        PatientActionDetailVO vo = new PatientActionDetailVO();
        fillActionInfo(vo, action, version);
        vo.setStatus(version.getStatus());
        vo.setAlgoConfig(version.getAlgoConfig());
        vo.setMotionSignature(version.getMotionSignature());
        vo.setPoseEngineId(version.getPoseEngineId());
        vo.setRules(actionRuleMapper.selectList(new LambdaQueryWrapper<ActionRule>()
                        .eq(ActionRule::getActionVersionId, version.getActionVersionId())
                        .orderByAsc(ActionRule::getActionRuleId))
                .stream().map(this::toRuleVO).collect(Collectors.toList()));
        return vo;
    }

    // ==================== 私有辅助 ====================

    private PatientActionVO toVO(Action action, ActionVersion version) {
        PatientActionVO vo = new PatientActionVO();
        fillActionInfo(vo, action, version);
        return vo;
    }

    private void fillActionInfo(PatientActionVO vo, Action action, ActionVersion version) {
        vo.setActionId(action.getActionId());
        vo.setActionVersionId(version.getActionVersionId());
        vo.setName(action.getName());
        vo.setDescription(action.getDescription());
        vo.setTargetPartCode(action.getTargetPartCode());
        vo.setPartName(resolvePartName(action.getTargetPartCode()));
        vo.setDifficulty(action.getDifficulty());
        try {
            vo.setDifficultyName(Difficulty.fromCode(action.getDifficulty()).getName());
        } catch (IllegalArgumentException e) {
            vo.setDifficultyName(action.getDifficulty());
        }
        vo.setActionType(version.getActionType());
        vo.setVersionNo(version.getVersionNo());
        vo.setUpdatedAt(version.getUpdatedAt());
    }

    private String resolvePartName(String partCode) {
        if (!StringUtils.hasText(partCode)) {
            return null;
        }
        RehabPartDict dict = rehabPartDictMapper.selectOne(new LambdaQueryWrapper<RehabPartDict>()
                .eq(RehabPartDict::getCode, partCode)
                .last("limit 1"));
        return dict != null ? dict.getName() : partCode;
    }

    private ActionRuleVO toRuleVO(ActionRule rule) {
        ActionRuleVO vo = new ActionRuleVO();
        vo.setActionRuleId(rule.getActionRuleId());
        vo.setJointName(rule.getJointName());
        vo.setKeypointIndices(rule.getKeypointIndices());
        vo.setAngleFormula(rule.getAngleFormula());
        vo.setJointRole(rule.getJointRole());
        vo.setDimension(rule.getDimension());
        vo.setPhase(rule.getPhase());
        vo.setMinAngle(rule.getMinAngle());
        vo.setMaxAngle(rule.getMaxAngle());
        vo.setStandardAngle(rule.getStandardAngle());
        vo.setToleranceSoft(rule.getToleranceSoft());
        vo.setToleranceHard(rule.getToleranceHard());
        vo.setHoldFramesRequired(rule.getHoldFramesRequired());
        vo.setSpeedMin(rule.getSpeedMin());
        vo.setSpeedMax(rule.getSpeedMax());
        vo.setScoreWeight(rule.getScoreWeight());
        vo.setSeverity(rule.getSeverity());
        vo.setPromptText(rule.getPromptText());
        vo.setSymmetryPairRuleId(rule.getSymmetryPairRuleId());
        return vo;
    }
}
