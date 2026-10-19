package com.rehab.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.admin.service.AdminActionTemplateService;
import com.rehab.model.mapper.action.ActionTemplateMapper;
import com.rehab.model.mapper.action.ActionVersionMapper;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.dto.ActionTemplateSaveRequest;
import com.rehab.model.entity.ActionTemplate;
import com.rehab.model.entity.ActionVersion;
import com.rehab.model.enums.TemplateStatus;
import com.rehab.admin.vo.AdminActionTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端 - 动作模板服务实现。
 */
@Service
@RequiredArgsConstructor
public class AdminActionTemplateServiceImpl extends ServiceImpl<ActionTemplateMapper, ActionTemplate>
        implements AdminActionTemplateService {

    private final ActionVersionMapper actionVersionMapper;

    @Override
    public PageResult<AdminActionTemplateVO> pageTemplates(Long versionId, Integer page, Integer size) {
        requireVersionExists(versionId);

        Page<ActionTemplate> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<ActionTemplate>()
                        .eq(ActionTemplate::getActionVersionId, versionId)
                        .orderByDesc(ActionTemplate::getCreatedAt));
        List<AdminActionTemplateVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(result, voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(Long versionId, ActionTemplateSaveRequest request) {
        requireVersionExists(versionId);

        ActionTemplate template = new ActionTemplate();
        template.setActionVersionId(versionId);
        template.setSourceType(request.getSourceType().getCode());
        // 数据库列为 NOT NULL，空值统一回落
        template.setFrameRate(request.getFrameRate() != null ? request.getFrameRate() : BigDecimal.valueOf(30));
        template.setFrameCount(request.getFrameCount() != null ? request.getFrameCount() : 0);
        template.setFeatureDim(request.getFeatureDim() != null ? request.getFeatureDim() : 0);
        template.setFeatureSequence(request.getFeatureSequence());
        template.setKeyframeIndex(StringUtils.hasText(request.getKeyframeIndex()) ? request.getKeyframeIndex() : "[]");
        template.setSampleCount(request.getSampleCount() != null ? request.getSampleCount() : 1);
        template.setStatus(TemplateStatus.ACTIVE.getCode());
        template.setCreatedBy(SecurityUtils.getUserId());
        this.save(template);
        return template.getTemplateId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long templateId) {
        ActionTemplate template = requireTemplate(templateId);
        if (TemplateStatus.ACTIVE.getCode().equals(template.getStatus())) {
            return; // 幂等
        }
        template.setStatus(TemplateStatus.ACTIVE.getCode());
        this.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        ActionTemplate template = requireTemplate(templateId);
        // 当前无独立模板引用表，直接逻辑删除
        this.removeById(template.getTemplateId());
    }

    private ActionVersion requireVersionExists(Long versionId) {
        ActionVersion version = actionVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(ResultCode.VERSION_NOT_FOUND, "动作版本不存在：" + versionId);
        }
        return version;
    }

    private ActionTemplate requireTemplate(Long templateId) {
        ActionTemplate template = this.getById(templateId);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动作模板不存在：" + templateId);
        }
        return template;
    }

    private AdminActionTemplateVO toVO(ActionTemplate t) {
        com.rehab.model.enums.TemplateSource source = com.rehab.model.enums.TemplateSource.fromCode(t.getSourceType());
        TemplateStatus status = TemplateStatus.fromCode(t.getStatus());
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
