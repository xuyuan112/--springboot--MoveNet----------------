package com.rehab.admin.service;

import com.rehab.admin.vo.BackupTaskVO;
import com.rehab.common.result.PageResult;
import com.rehab.model.dto.BackupRestoreRequest;

/**
 * 管理端 - 数据库备份与恢复服务。
 *
 * <p>备份异步执行，备份/恢复全局互斥（Redis 锁，并发返回 423 RESOURCE_LOCKED）；
 * 恢复必须携带一次性 confirmToken 与原因，恢复前自动生成安全快照并全程审计。</p>
 */
public interface AdminBackupService {

    /** 创建备份任务（异步执行），返回备份任务ID */
    Long createBackup();

    /** 分页查询备份任务 */
    PageResult<BackupTaskVO> pageBackups(Integer page, Integer size, String status, String taskType);

    /**
     * 查询备份详情：对 BACKUP 且 SUCCESS 的任务下发一次性恢复确认令牌（TTL 5 分钟）。
     */
    BackupTaskVO getBackupDetail(Long backupId);

    /** 二次确认后恢复：校验令牌与原因 → 恢复前快照 → 执行恢复 → 审计 */
    void restore(Long backupId, BackupRestoreRequest request);
}
