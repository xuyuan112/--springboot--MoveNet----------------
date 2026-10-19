package com.rehab.app.listener;

import com.rehab.model.mapper.system.OperationLogMapper;
import com.rehab.common.event.OperationLogEvent;
import com.rehab.model.entity.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 操作日志事件监听器：common 层发布的审计事件在此落库 operation_log。
 * 同步监听、失败仅记录错误日志，不抛出异常影响主业务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogEventListener {

    private final OperationLogMapper operationLogMapper;

    @EventListener
    public void onOperationLog(OperationLogEvent event) {
        try {
            OperationLog entity = new OperationLog();
            entity.setUserId(event.getUserId());
            entity.setModule(event.getModule());
            entity.setAction(event.getAction());
            entity.setTargetType(StringUtils.hasText(event.getTargetType()) ? event.getTargetType() : "");
            entity.setIp(StringUtils.hasText(event.getIp()) ? event.getIp() : "unknown");
            entity.setDeviceId(event.getDeviceId());
            entity.setStatus(event.getStatus());
            entity.setDetail(event.getDetail());
            // created_at 由数据库默认值生成
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("操作日志落库失败 module={}, action={}", event.getModule(), event.getAction(), e);
        }
    }
}
