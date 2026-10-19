package com.rehab.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.admin.service.AdminActionVersionService;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.action.ActionRuleMapper;
import com.rehab.model.mapper.action.ActionTemplateMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.dto.ActionVersionSaveRequest;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.ActionRule;
import com.rehab.model.entity.ActionTemplate;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.enums.ActionStatus;
import com.rehab.model.enums.ActionType;
import com.rehab.model.enums.VersionStatus;
import com.rehab.admin.vo.AdminActionRuleVO;
import com.rehab.admin.vo.AdminActionTemplateVO;
import com.rehab.admin.vo.AdminActionVersionDetailVO;
import com.rehab.admin.vo.AdminActionVersionVO;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端 - 动作版本服务实现。
 */
@Service
@RequiredArgsConstructor
public class AdminActionVersionServiceImpl extends ServiceImpl<ActionVersionMapper, ActionVersion>
        implements AdminActionVersionService {

    private final ActionMapper actionMapper;
    private final ActionRuleMapper actionRuleMapper;
    private final ActionTemplateMapper actionTemplateMapper;

    @Override
    public PageResult<AdminActionVersionVO> pageVersions(Integer page, Integer size,
                                                         Long actionId, String status) {
        LambdaQueryWrapper<ActionVersion> wrapper = new LambdaQueryWrapper<ActionVersion>()
                .eq(actionId != null, ActionVersion::getActionId, actionId)
                .eq(StringUtils.hasText(status), ActionVersion::getStatus, status)
                .orderByDesc(ActionVersion::getVersionNo);

        Page<ActionVersion> result = this.page(new Page<>(page, size), wrapper);
        List<AdminActionVersionVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(result, voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVersion(Long actionId, ActionVersionSaveRequest request) {
        requireActionExists(actionId);
        validateConfigJson(request.getAlgoConfig(), request.getMotionSignature());

        Integer maxVersionNo = this.baseMapper.selectMaxVersionNo(actionId);
        int nextVersionNo = (maxVersionNo == null ? 0 : maxVersionNo) + 1;

        ActionVersion version = new ActionVersion();
        version.setActionId(actionId);
        version.setVersionNo(nextVersionNo);
        version.setActionType(request.getActionType().getCode());
        version.setPoseEngineId(request.getPoseEngineId());
        // 数据库列为 NOT NULL，空值统一回落为空 JSON 对象
        version.setAlgoConfig(StringUtils.hasText(request.getAlgoConfig()) ? request.getAlgoConfig() : "{}");
        version.setMotionSignature(StringUtils.hasText(request.getMotionSignature()) ? request.getMotionSignature() : "{}");
        version.setStatus(VersionStatus.DRAFT.getCode());
        version.setCreatedBy(SecurityUtils.getUserId());
        this.save(version);
        return version.getActionVersionId();
    }

    @Override
    public AdminActionVersionDetailVO detail(Long versionId) {
        ActionVersion version = requireVersion(versionId);
        List<AdminActionRuleVO> rules = listRules(versionId);
        List<AdminActionTemplateVO> templates = listTemplates(versionId);
        return toDetailVO(version, rules, templates);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVersion(Long versionId, ActionVersionSaveRequest request) {
        ActionVersion version = requireVersion(versionId);
        if (!VersionStatus.DRAFT.getCode().equals(version.getStatus())) {
            throw new BusinessException(ResultCode.VERSION_IMMUTABLE,
                    "已发布版本不可修改，当前状态：" + version.getStatus());
        }
        validateConfigJson(request.getAlgoConfig(), request.getMotionSignature());
        version.setActionType(request.getActionType().getCode());
        version.setPoseEngineId(request.getPoseEngineId());
        version.setAlgoConfig(StringUtils.hasText(request.getAlgoConfig()) ? request.getAlgoConfig() : "{}");
        version.setMotionSignature(StringUtils.hasText(request.getMotionSignature()) ? request.getMotionSignature() : "{}");
        this.updateById(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long versionId) {
        ActionVersion version = requireVersion(versionId);
        if (!VersionStatus.DRAFT.getCode().equals(version.getStatus())) {
            throw new BusinessException(ResultCode.VERSION_IMMUTABLE,
                    "仅草稿版本可发布，当前状态：" + version.getStatus());
        }
        if (!StringUtils.hasText(version.getPoseEngineId())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "发布前必须配置姿态引擎标识");
        }
        // 发布前复检库存配置必须为合法 JSON，防止脏数据流入评估引擎
        if (!JSONUtil.isJson(version.getAlgoConfig())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "算法配置不是合法 JSON，无法发布");
        }
        if (!JSONUtil.isJson(version.getMotionSignature())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "运动签名不是合法 JSON，无法发布");
        }

        // 校验该版本下所有规则 scoreWeight 之和 = 1
        BigDecimal totalWeight = actionRuleMapper.sumScoreWeightByVersionId(versionId);
        if (totalWeight == null) {
            totalWeight = BigDecimal.ZERO;
        }
        BigDecimal expected = BigDecimal.ONE;
        if (totalWeight.compareTo(expected) != 0) {
            throw new BusinessException(ResultCode.PLAN_PARAMETER_INVALID,
                    "发布失败：规则权重和必须等于 1，当前为 " + totalWeight);
        }

        version.setStatus(VersionStatus.PUBLISHED.getCode());
        this.updateById(version);

        // 若所属动作仍为 DRAFT，同步改为 PUBLISHED
        Action action = actionMapper.selectById(version.getActionId());
        if (action != null && ActionStatus.DRAFT.getCode().equals(action.getStatus())) {
            action.setStatus(ActionStatus.PUBLISHED.getCode());
            actionMapper.updateById(action);
        }
    }

    private ActionVersion requireVersion(Long versionId) {
        ActionVersion version = this.getById(versionId);
        if (version == null) {
            throw new BusinessException(ResultCode.VERSION_NOT_FOUND, "动作版本不存在：" + versionId);
        }
        return version;
    }

    private void requireActionExists(Long actionId) {
        if (actionMapper.selectById(actionId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动作不存在：" + actionId);
        }
    }

    /**
     * 校验可选的 JSON 配置字段：为空时回落为 "{}"（允许不填），非空时必须为合法 JSON。
     */
    private void validateConfigJson(String algoConfig, String motionSignature) {
        if (StringUtils.hasText(algoConfig) && !JSONUtil.isJson(algoConfig)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "算法配置不是合法 JSON");
        }
        if (StringUtils.hasText(motionSignature) && !JSONUtil.isJson(motionSignature)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "运动签名不是合法 JSON");
        }
    }

    private List<AdminActionRuleVO> listRules(Long versionId) {
        List<ActionRule> rules = actionRuleMapper.selectList(
                new LambdaQueryWrapper<ActionRule>()
                        .eq(ActionRule::getActionVersionId, versionId)
                        .orderByAsc(ActionRule::getCreatedAt));
        return rules.stream().map(this::toRuleVO).toList();
    }

    private List<AdminActionTemplateVO> listTemplates(Long versionId) {
        List<ActionTemplate> templates = actionTemplateMapper.selectList(
                new LambdaQueryWrapper<ActionTemplate>()
                        .eq(ActionTemplate::getActionVersionId, versionId)
                        .orderByDesc(ActionTemplate::getCreatedAt));
        return templates.stream().map(this::toTemplateVO).toList();
    }

    private AdminActionVersionVO toVO(ActionVersion v) {
        ActionType type = ActionType.fromCode(v.getActionType());
        VersionStatus status = VersionStatus.fromCode(v.getStatus());
        return AdminActionVersionVO.builder()
                .versionId(v.getActionVersionId())
                .actionId(v.getActionId())
                .versionNo(v.getVersionNo())
                .actionType(type.getCode())
                .actionTypeName(type.getName())
                .poseEngineId(v.getPoseEngineId())
                .status(status.getCode())
                .statusName(status.getName())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private AdminActionVersionDetailVO toDetailVO(ActionVersion v,
                                                   List<AdminActionRuleVO> rules,
                                                   List<AdminActionTemplateVO> templates) {
        ActionType type = ActionType.fromCode(v.getActionType());
        VersionStatus status = VersionStatus.fromCode(v.getStatus());
        return AdminActionVersionDetailVO.builder()
                .versionId(v.getActionVersionId())
                .actionId(v.getActionId())
                .versionNo(v.getVersionNo())
                .actionType(type.getCode())
                .actionTypeName(type.getName())
                .poseEngineId(v.getPoseEngineId())
                .status(status.getCode())
                .statusName(status.getName())
                .algoConfig(v.getAlgoConfig())
                .motionSignature(v.getMotionSignature())
                .createdBy(v.getCreatedBy())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .ruleList(rules)
                .templateList(templates)
                .build();
    }

    private AdminActionRuleVO toRuleVO(ActionRule r) {
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

    private AdminActionTemplateVO toTemplateVO(ActionTemplate t) {
        com.rehab.model.enums.TemplateSource source = com.rehab.model.enums.TemplateSource.fromCode(t.getSourceType());
        com.rehab.model.enums.TemplateStatus status = com.rehab.model.enums.TemplateStatus.fromCode(t.getStatus());
        return AdminActionTemplateVO.builder()
                .templateId(t.getTemplateId())
                .actionVersionId(t.getActionVersionId())
                .sourceType(source.getCode())
                .sourceTypeName(source.getName())
                .frameRate(t.getFrameRate())
                .frameCount(t.getFrameCount())
                .featureDim(t.getFeatureDim())
                .featureSequence(t.getFeatureSequence())
                .keyframeIndex(t.getKeyframeIndex())
                .sampleCount(t.getSampleCount())
                .status(status.getCode())
                .statusName(status.getName())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
