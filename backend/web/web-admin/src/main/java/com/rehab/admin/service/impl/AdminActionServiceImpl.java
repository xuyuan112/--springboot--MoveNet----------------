package com.rehab.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.admin.service.AdminActionService;
import com.rehab.model.mapper.action.ActionMapper;
import com.rehab.model.mapper.system.RehabPartDictMapper;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.dto.ActionSaveRequest;
import com.rehab.model.entity.Action;
import com.rehab.model.entity.RehabPartDict;
import com.rehab.model.enums.ActionStatus;
import com.rehab.model.enums.Difficulty;
import com.rehab.admin.vo.AdminActionDetailVO;
import com.rehab.admin.vo.AdminActionVO;
import com.rehab.admin.vo.AdminActionVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端 - 动作基础信息服务实现。
 */
@Service
@RequiredArgsConstructor
public class AdminActionServiceImpl extends ServiceImpl<ActionMapper, Action>
        implements AdminActionService {

    private final RehabPartDictMapper rehabPartDictMapper;

    @Override
    public PageResult<AdminActionVO> pageActions(Integer page, Integer size,
                                                 String targetPart, String status, String keyword) {
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<Action>()
                .eq(StringUtils.hasText(targetPart), Action::getTargetPartCode, targetPart)
                .eq(StringUtils.hasText(status), Action::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Action::getName, keyword)
                        .or().like(Action::getDescription, keyword))
                .orderByDesc(Action::getActionId);

        Page<Action> result = this.page(new Page<>(page, size), wrapper);
        if (result.getRecords().isEmpty()) {
            return PageResult.of(result, List.of());
        }
        // 批量查询部位名称，避免 N+1
        Map<String, String> partNameMap = loadPartNames(result.getRecords());
        List<AdminActionVO> voList = result.getRecords().stream()
                .map(a -> toVO(a, partNameMap))
                .toList();
        return PageResult.of(result, voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAction(ActionSaveRequest request) {
        requirePartExists(request.getTargetPartCode());

        Action action = new Action();
        action.setName(request.getName());
        action.setTargetPartCode(request.getTargetPartCode());
        action.setDifficulty(request.getDifficulty().getCode());
        action.setDescription(request.getDescription());
        action.setStatus(ActionStatus.DRAFT.getCode());
        action.setCreatedBy(SecurityUtils.getUserId());
        this.save(action);
        return action.getActionId();
    }

    @Override
    public AdminActionDetailVO detail(Long actionId) {
        Action action = requireAction(actionId);
        Map<String, String> partNameMap = loadPartNames(List.of(action));
        AdminActionDetailVO vo = toDetailVO(action, partNameMap);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAction(Long actionId, ActionSaveRequest request) {
        Action action = requireAction(actionId);
        if (!ActionStatus.DRAFT.getCode().equals(action.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT,
                    "仅草稿状态的动作可编辑基础信息，当前状态：" + action.getStatus());
        }
        requirePartExists(request.getTargetPartCode());

        action.setName(request.getName());
        action.setTargetPartCode(request.getTargetPartCode());
        action.setDifficulty(request.getDifficulty().getCode());
        action.setDescription(request.getDescription());
        this.updateById(action);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long actionId) {
        Action action = requireAction(actionId);
        if (ActionStatus.DISABLED.getCode().equals(action.getStatus())) {
            return; // 幂等
        }
        action.setStatus(ActionStatus.DISABLED.getCode());
        this.updateById(action);
    }

    private Action requireAction(Long actionId) {
        Action action = this.getById(actionId);
        if (action == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动作不存在：" + actionId);
        }
        return action;
    }

    private void requirePartExists(String targetPartCode) {
        RehabPartDict dict = rehabPartDictMapper.selectById(targetPartCode);
        if (dict == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "目标部位编码不存在：" + targetPartCode);
        }
    }

    private Map<String, String> loadPartNames(List<Action> actions) {
        List<String> codes = actions.stream()
                .map(Action::getTargetPartCode)
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            return Map.of();
        }
        return rehabPartDictMapper.selectBatchIds(codes).stream()
                .collect(Collectors.toMap(RehabPartDict::getCode, RehabPartDict::getName));
    }

    private AdminActionVO toVO(Action action, Map<String, String> partNameMap) {
        Difficulty difficulty = Difficulty.fromCode(action.getDifficulty());
        ActionStatus status = ActionStatus.fromCode(action.getStatus());
        return AdminActionVO.builder()
                .actionId(action.getActionId())
                .name(action.getName())
                .targetPartCode(action.getTargetPartCode())
                .targetPartName(partNameMap.getOrDefault(action.getTargetPartCode(), action.getTargetPartCode()))
                .difficulty(difficulty.getCode())
                .difficultyName(difficulty.getName())
                .status(status.getCode())
                .statusName(status.getName())
                .createdAt(action.getCreatedAt())
                .build();
    }

    private AdminActionDetailVO toDetailVO(Action action, Map<String, String> partNameMap) {
        Difficulty difficulty = Difficulty.fromCode(action.getDifficulty());
        ActionStatus status = ActionStatus.fromCode(action.getStatus());
        return AdminActionDetailVO.builder()
                .actionId(action.getActionId())
                .name(action.getName())
                .targetPartCode(action.getTargetPartCode())
                .targetPartName(partNameMap.getOrDefault(action.getTargetPartCode(), action.getTargetPartCode()))
                .difficulty(difficulty.getCode())
                .difficultyName(difficulty.getName())
                .status(status.getCode())
                .statusName(status.getName())
                .description(action.getDescription())
                .createdBy(action.getCreatedBy())
                .createdAt(action.getCreatedAt())
                .updatedAt(action.getUpdatedAt())
                .versionList(List.of())
                .build();
    }
}
