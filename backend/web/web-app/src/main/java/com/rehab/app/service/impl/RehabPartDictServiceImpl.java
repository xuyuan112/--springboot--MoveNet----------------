package com.rehab.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.content.HealthContentMapper;
import com.rehab.model.mapper.plan.RehabPlanMapper;
import com.rehab.model.mapper.account.PatientRehabPartMapper;
import com.rehab.model.mapper.system.RehabPartDictMapper;
import com.rehab.app.service.RehabPartDictService;
import com.rehab.common.constant.Constants;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.model.dto.RehabPartDictDTO;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.HealthContent;
import com.rehab.model.entity.PatientRehabPart;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.model.entity.RehabPlan;
import com.rehab.app.vo.RehabPartDictVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 康复部位字典服务实现。
 */
@Service
@RequiredArgsConstructor
public class RehabPartDictServiceImpl
        extends ServiceImpl<RehabPartDictMapper, RehabPartDict>
        implements RehabPartDictService {

    private final ActionMapper actionMapper;
    private final HealthContentMapper healthContentMapper;
    private final RehabPlanMapper rehabPlanMapper;
    private final PatientRehabPartMapper patientRehabPartMapper;

    @Override
    public PageResult<RehabPartDictVO> pageDict(Integer page, Integer size,
                                                String groupCode, String keyword) {
        LambdaQueryWrapper<RehabPartDict> wrapper = new LambdaQueryWrapper<RehabPartDict>()
                .eq(StringUtils.hasText(groupCode), RehabPartDict::getGroupCode, groupCode)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(RehabPartDict::getName, keyword)
                        .or()
                        .like(RehabPartDict::getCode, keyword))
                .orderByAsc(RehabPartDict::getSortNo)
                .orderByAsc(RehabPartDict::getCode);

        Page<RehabPartDict> result = this.page(new Page<>(page, size), wrapper);
        List<RehabPartDictVO> voList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(result, voList);
    }

    @Override
    public List<RehabPartDictVO> listEnabled() {
        return this.list(new LambdaQueryWrapper<RehabPartDict>()
                        .eq(RehabPartDict::getEnabled, Constants.ENABLED)
                        .orderByAsc(RehabPartDict::getSortNo))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public void create(RehabPartDictDTO dto) {
        if (this.getById(dto.getCode()) != null) {
            throw new BusinessException(ResultCode.DUPLICATE_RESOURCE, "部位编码已存在：" + dto.getCode());
        }
        RehabPartDict entity = new RehabPartDict();
        entity.setCode(dto.getCode());
        applyDto(entity, dto);
        this.save(entity);
    }

    @Override
    public void update(String code, RehabPartDictDTO dto) {
        RehabPartDict entity = this.getById(code);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部位编码不存在：" + code);
        }
        applyDto(entity, dto);
        // 编码为业务主键，不允许修改
        entity.setCode(code);
        this.updateById(entity);
    }

    @Override
    public void removeDict(String code) {
        RehabPartDict entity = this.getById(code);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部位编码不存在：" + code);
        }
        // 引用计数校验：动作库 / 健康内容 / 康复计划 / 患者登记部位四表
        // （各表 deleted 字段由 @TableLogic 自动过滤，只统计有效数据）
        long actionCount = actionMapper.selectCount(
                new LambdaQueryWrapper<Action>().eq(Action::getTargetPartCode, code));
        if (actionCount > 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT,
                    "该部位已被 " + actionCount + " 个动作引用，无法删除");
        }
        long contentCount = healthContentMapper.selectCount(
                new LambdaQueryWrapper<HealthContent>().eq(HealthContent::getTargetPartCode, code));
        if (contentCount > 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT,
                    "该部位已被 " + contentCount + " 篇内容引用，无法删除");
        }
        long planCount = rehabPlanMapper.selectCount(
                new LambdaQueryWrapper<RehabPlan>().eq(RehabPlan::getRehabPartCode, code));
        if (planCount > 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT,
                    "该部位已被 " + planCount + " 个康复计划引用，无法删除");
        }
        long patientCount = patientRehabPartMapper.selectCount(
                new LambdaQueryWrapper<PatientRehabPart>().eq(PatientRehabPart::getRehabPartCode, code));
        if (patientCount > 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT,
                    "该部位已被 " + patientCount + " 位患者登记，无法删除");
        }
        this.removeById(code);
    }

    private void applyDto(RehabPartDict entity, RehabPartDictDTO dto) {
        entity.setName(dto.getName());
        entity.setGroupCode(dto.getGroupCode().getCode());
        entity.setSortNo(dto.getSortNo());
        entity.setEnabled(Boolean.TRUE.equals(dto.getEnabled()) ? Constants.ENABLED : Constants.DISABLED);
    }

    private RehabPartDictVO toVO(RehabPartDict entity) {
        RehabPartDictVO vo = new RehabPartDictVO();
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setGroupCode(entity.getGroupCode());
        vo.setSortNo(entity.getSortNo());
        vo.setEnabled(Constants.ENABLED.equals(entity.getEnabled()));
        return vo;
    }
}
