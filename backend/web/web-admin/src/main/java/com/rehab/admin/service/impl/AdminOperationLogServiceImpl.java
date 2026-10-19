package com.rehab.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.admin.service.AdminOperationLogService;
import com.rehab.admin.vo.AdminOperationLogVO;
import com.rehab.common.result.PageResult;
import com.rehab.model.entity.OperationLog;
import com.rehab.model.entity.UserAccount;
import com.rehab.model.mapper.account.UserAccountMapper;
import com.rehab.model.mapper.system.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端 - 操作审计日志只读查询实现。
 */
@Service
@RequiredArgsConstructor
public class AdminOperationLogServiceImpl
        extends ServiceImpl<OperationLogMapper, OperationLog>
        implements AdminOperationLogService {

    private final UserAccountMapper userAccountMapper;

    @Override
    public PageResult<AdminOperationLogVO> pageLogs(Integer page, Integer size,
                                                    String module, Long operatorId,
                                                    LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(StringUtils.hasText(module), OperationLog::getModule, module)
                .eq(operatorId != null, OperationLog::getUserId, operatorId)
                .ge(from != null, OperationLog::getCreatedAt, from)
                .le(to != null, OperationLog::getCreatedAt, to)
                .orderByDesc(OperationLog::getLogId);

        Page<OperationLog> result = this.page(new Page<>(page, size), wrapper);
        List<AdminOperationLogVO> voList = result.getRecords().stream().map(this::toVO).toList();
        fillUserNames(voList);
        return PageResult.of(result, voList);
    }

    private AdminOperationLogVO toVO(OperationLog entity) {
        AdminOperationLogVO vo = new AdminOperationLogVO();
        vo.setLogId(entity.getLogId());
        vo.setUserId(entity.getUserId());
        vo.setModule(entity.getModule());
        vo.setAction(entity.getAction());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setIp(entity.getIp());
        vo.setDeviceId(entity.getDeviceId());
        vo.setStatus(entity.getStatus());
        vo.setDetail(entity.getDetail());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private void fillUserNames(List<AdminOperationLogVO> voList) {
        Set<Long> userIds = voList.stream()
                .map(AdminOperationLogVO::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = userAccountMapper.selectList(
                        new LambdaQueryWrapper<UserAccount>().in(UserAccount::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(UserAccount::getUserId, UserAccount::getUsername, (a, b) -> a));
        voList.forEach(vo -> vo.setUserName(
                vo.getUserId() == null ? "系统" : nameMap.getOrDefault(vo.getUserId(), "用户#" + vo.getUserId())));
    }
}
