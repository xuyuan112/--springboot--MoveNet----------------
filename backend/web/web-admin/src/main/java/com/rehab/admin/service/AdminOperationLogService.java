package com.rehab.admin.service;

import com.rehab.admin.vo.AdminOperationLogVO;
import com.rehab.common.result.PageResult;

import java.time.LocalDateTime;

/**
 * 管理端 - 操作审计日志只读查询。
 * 查询本身不写 operation_log，避免审计接口自记录噪音。
 */
public interface AdminOperationLogService {

    /**
     * 分页查询审计日志。
     *
     * @param module     模块名（可选）
     * @param operatorId 操作人用户ID（可选）
     * @param from       起始时间（可选，包含）
     * @param to         截止时间（可选，包含）
     */
    PageResult<AdminOperationLogVO> pageLogs(Integer page, Integer size,
                                            String module, Long operatorId,
                                            LocalDateTime from, LocalDateTime to);
}
