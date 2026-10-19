package com.rehab.app.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.app.service.UserLoginLogService;
import com.rehab.app.vo.LoginLogVO;
import com.rehab.common.result.PageResult;
import com.rehab.common.security.SecurityUtils;
import com.rehab.model.entity.OperationLog;
import com.rehab.model.mapper.system.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 本人登录日志查询服务实现（三端共用，只读）。
 */
@Service
@RequiredArgsConstructor
public class UserLoginLogServiceImpl implements UserLoginLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public PageResult<LoginLogVO> pageMyLoginLogs(Integer page, Integer size) {
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getModule, "AUTH")
                .eq(OperationLog::getUserId, userId)
                .orderByDesc(OperationLog::getCreatedAt)
                .orderByDesc(OperationLog::getLogId);

        Page<OperationLog> result = operationLogMapper.selectPage(new Page<>(page, size), wrapper);
        List<LoginLogVO> voList = result.getRecords().stream()
                .map(log -> LoginLogVO.builder()
                        .logId(log.getLogId())
                        .action(log.getAction())
                        .status(log.getStatus())
                        .ip(log.getIp())
                        .deviceId(log.getDeviceId())
                        .message(extractMessage(log.getDetail()))
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
        return PageResult.of(result, voList);
    }

    /** 从审计 detail JSON 中提取 message 字段；非 JSON 时原样返回。 */
    private String extractMessage(String detail) {
        if (!StringUtils.hasText(detail)) {
            return null;
        }
        try {
            String message = JSONUtil.parseObj(detail).getStr("message");
            return StringUtils.hasText(message) ? message : detail;
        } catch (Exception e) {
            return detail;
        }
    }
}
