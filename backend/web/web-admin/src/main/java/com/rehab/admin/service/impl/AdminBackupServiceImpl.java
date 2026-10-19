package com.rehab.admin.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rehab.admin.config.BackupExecutorConfig;
import com.rehab.admin.config.BackupProperties;
import com.rehab.admin.service.AdminBackupService;
import com.rehab.admin.service.backup.SqlBackupEngine;
import com.rehab.admin.vo.BackupTaskVO;
import com.rehab.common.constant.Constants;
import com.rehab.common.constant.RedisKeyConstants;
import com.rehab.common.event.OperationLogEvent;
import com.rehab.common.exception.BusinessException;
import com.rehab.common.result.PageResult;
import com.rehab.common.result.ResultCode;
import com.rehab.common.security.SecurityUtils;
import com.rehab.common.util.ServletUtils;
import com.rehab.model.dto.BackupRestoreRequest;
import com.rehab.model.entity.DbBackupTask;
import com.rehab.model.entity.UserAccount;
import com.rehab.model.mapper.account.UserAccountMapper;
import com.rehab.model.mapper.system.DbBackupTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 管理端 - 数据库备份与恢复服务实现。
 */
@Slf4j
@Service
public class AdminBackupServiceImpl
        extends ServiceImpl<DbBackupTaskMapper, DbBackupTask>
        implements AdminBackupService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 备份/恢复互斥锁 TTL（兜底，正常流程主动释放） */
    private static final Duration LOCK_TTL = Duration.ofHours(2);
    /** 恢复确认令牌 TTL */
    private static final Duration CONFIRM_TTL = Duration.ofMinutes(5);
    /** 错误信息列长度上限 */
    private static final int ERROR_MSG_MAX = 1000;

    private final BackupProperties backupProperties;
    private final SqlBackupEngine backupEngine;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserAccountMapper userAccountMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Executor backupExecutor;

    public AdminBackupServiceImpl(BackupProperties backupProperties,
                                  SqlBackupEngine backupEngine,
                                  StringRedisTemplate stringRedisTemplate,
                                  UserAccountMapper userAccountMapper,
                                  ApplicationEventPublisher eventPublisher,
                                  @Qualifier(BackupExecutorConfig.BACKUP_EXECUTOR) Executor backupExecutor) {
        this.backupProperties = backupProperties;
        this.backupEngine = backupEngine;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userAccountMapper = userAccountMapper;
        this.eventPublisher = eventPublisher;
        this.backupExecutor = backupExecutor;
    }

    @Override
    public Long createBackup() {
        Long userId = SecurityUtils.getUserId();
        String lockValue = UUID.randomUUID().toString();
        if (!acquireLock(lockValue)) {
            throw new BusinessException(ResultCode.RESOURCE_LOCKED, "已有备份或恢复任务正在执行，请稍后再试");
        }
        DbBackupTask task = new DbBackupTask();
        task.setTaskType("BACKUP");
        task.setStatus("RUNNING");
        task.setFileSize(0L);
        task.setTriggeredBy(userId);
        task.setStartedAt(LocalDateTime.now());
        this.save(task);

        final Long backupId = task.getBackupId();
        try {
            backupExecutor.execute(() -> runBackup(backupId, lockValue));
        } catch (Exception e) {
            // 线程池拒绝（兜底）：标记失败并释放锁
            markFailed(backupId, "备份任务提交失败：" + e.getMessage());
            releaseLock(lockValue);
            throw new BusinessException(ResultCode.RESOURCE_LOCKED, "备份任务排队失败，请稍后再试");
        }
        return backupId;
    }

    @Override
    public PageResult<BackupTaskVO> pageBackups(Integer page, Integer size, String status, String taskType) {
        LambdaQueryWrapper<DbBackupTask> wrapper = new LambdaQueryWrapper<DbBackupTask>()
                .eq(StringUtils.hasText(status), DbBackupTask::getStatus, status)
                .eq(StringUtils.hasText(taskType), DbBackupTask::getTaskType, taskType)
                .orderByDesc(DbBackupTask::getBackupId);
        Page<DbBackupTask> result = this.page(new Page<>(page, size), wrapper);
        List<BackupTaskVO> voList = result.getRecords().stream().map(this::toVO).toList();
        fillOperatorNames(voList);
        return PageResult.of(result, voList);
    }

    @Override
    public BackupTaskVO getBackupDetail(Long backupId) {
        DbBackupTask task = requireTask(backupId);
        BackupTaskVO vo = toVO(task);
        fillOperatorNames(List.of(vo));
        // 成功且文件存在的备份/快照均可下发一次性确认令牌（快照用于失败后回退）
        if ("SUCCESS".equals(task.getStatus())
                && task.getFilePath() != null && Files.exists(Paths.get(task.getFilePath()))) {
            String token = UUID.randomUUID().toString().replace("-", "");
            stringRedisTemplate.opsForValue().set(confirmKey(backupId), token, CONFIRM_TTL);
            vo.setConfirmToken(token);
        }
        return vo;
    }

    @Override
    public void restore(Long backupId, BackupRestoreRequest request) {
        DbBackupTask task = requireTask(backupId);
        boolean snapshotRollback = "PRE_RESTORE_SNAPSHOT".equals(task.getTaskType());
        if (!"BACKUP".equals(task.getTaskType()) && !snapshotRollback) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "不支持的备份任务类型：" + task.getTaskType());
        }
        if (!"SUCCESS".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅成功的备份任务可恢复");
        }
        Path backupFile = Paths.get(task.getFilePath());
        if (!Files.exists(backupFile)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "备份文件不存在或已被移动：" + task.getFilePath());
        }

        // 一次性确认令牌校验（取出即删，杜绝重放）
        String key = confirmKey(backupId);
        String stored = stringRedisTemplate.opsForValue().get(key);
        if (stored == null || !stored.equals(request.getConfirmToken().trim())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "确认令牌无效或已过期，请重新获取确认码");
        }
        stringRedisTemplate.delete(key);

        Long userId = SecurityUtils.getUserId();
        String lockValue = UUID.randomUUID().toString();
        if (!acquireLock(lockValue)) {
            throw new BusinessException(ResultCode.RESOURCE_LOCKED, "已有备份或恢复任务正在执行，请稍后再试");
        }
        // 普通备份恢复前生成的安全快照（快照回退不再递归建快照）
        DbBackupTask snapshotTask = null;
        try {
            // 1) 普通备份恢复：先做恢复前安全快照（同步，失败则中止恢复，绝不带风险恢复）
            if (!snapshotRollback) {
                snapshotTask = createPreRestoreSnapshot(backupId, request.getReason(), userId);
            }

            // 2) 执行恢复
            backupEngine.restore(backupFile);

            // 3) 恢复脚本会把 db_backup_task 一并回滚到备份时点，重建任务表的现实状态：
            //    目标任务在备份文件中停留在 RUNNING 且文件元数据缺失；本次新建快照在备份时点尚不存在
            Long finalSnapshotId = rebuildTaskStateAfterRestore(task, snapshotTask);

            // 4) 恢复完成后补写审计（恢复脚本会回滚 operation_log 到备份时点，必须在恢复后写入）
            if (snapshotRollback) {
                publishAudit("BACKUP", "RESTORE", "DB_BACKUP", JSONUtil.createObj()
                        .set("kind", "SNAPSHOT_ROLLBACK")
                        .set("snapshotId", backupId)
                        .set("reason", request.getReason())
                        .toString());
            } else {
                publishAudit("BACKUP", "RESTORE", "DB_BACKUP", JSONUtil.createObj()
                        .set("backupId", backupId)
                        .set("snapshotId", finalSnapshotId)
                        .set("reason", request.getReason())
                        .toString());
                publishAudit("BACKUP", "RESTORE_SNAPSHOT", "DB_BACKUP", JSONUtil.createObj()
                        .set("snapshotId", finalSnapshotId)
                        .set("sourceBackupId", backupId)
                        .set("reason", request.getReason())
                        .toString());
            }
            log.info("备份恢复完成 targetId={}, snapshotRollback={}, snapshotId={}, operator={}",
                    backupId, snapshotRollback, finalSnapshotId, userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("备份恢复失败 targetId={}, snapshotId={}", backupId,
                    snapshotTask == null ? null : snapshotTask.getBackupId(), e);
            // 库可能已被部分恢复：尽力把任务记录重建回可读状态，便于按快照回退
            try {
                rebuildTaskStateAfterRestore(task, snapshotTask);
            } catch (Exception rebuildEx) {
                log.error("恢复失败后重建任务表状态异常", rebuildEx);
            }
            // 恢复失败也留痕（此时库可能处于不一致状态，单独建的快照可用于补救）
            safePublishAudit("BACKUP", "RESTORE_FAIL", "DB_BACKUP", JSONUtil.createObj()
                    .set("backupId", backupId)
                    .set("snapshotId", snapshotTask == null ? null : snapshotTask.getBackupId())
                    .set("reason", request.getReason())
                    .set("error", truncate(e.getMessage(), ERROR_MSG_MAX))
                    .toString());
            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                    "恢复执行失败：" + truncate(e.getMessage(), 200)
                            + "；可使用恢复前快照（ID=" + (snapshotTask == null ? null : snapshotTask.getBackupId())
                            + "）回退");
        } finally {
            releaseLock(lockValue);
        }
    }

    /**
     * 恢复完成后重建 db_backup_task 的现实状态。
     * 1) 目标任务在备份文件中停留在 RUNNING 且无文件信息，写回成功态与文件元数据；
     * 2) 本次恢复前新建的快照在备份时点不存在，按原元数据重新落库（自增 ID 可能变化，返回最终 ID）。
     *
     * @return 本次恢复前快照在库中的最终 ID（无快照时为 null）
     */
    private Long rebuildTaskStateAfterRestore(DbBackupTask target, DbBackupTask snapshot) {
        // 目标任务：优先按主键更新；行被回滚消失时兜底新增
        if (this.getById(target.getBackupId()) != null) {
            this.updateById(target);
        } else {
            target.setBackupId(null);
            this.save(target);
        }

        if (snapshot == null) {
            return null;
        }
        if (this.getById(snapshot.getBackupId()) == null) {
            // 备份时点不存在该行：清空主键重新插入，保留文件路径/校验值等全部元数据
            snapshot.setBackupId(null);
            this.save(snapshot);
        } else {
            this.updateById(snapshot);
        }
        return snapshot.getBackupId();
    }

    // ==================== 备份任务执行 ====================

    /** 异步执行备份并落结果 */
    private void runBackup(Long backupId, String lockValue) {
        Path file = null;
        try {
            DbBackupTask task = this.getById(backupId);
            String fileName = "backup_" + backupId + "_" + LocalDateTime.now().format(FILE_TS) + ".sql";
            file = Paths.get(backupProperties.getDir()).toAbsolutePath().resolve(fileName);
            SqlBackupEngine.BackupResult result = backupEngine.backup(
                    file, "backup_id=" + backupId);

            task.setStatus("SUCCESS");
            task.setFilePath(file.toString());
            task.setFileSize(result.getFileSize());
            task.setSha256(result.getSha256());
            task.setFinishedAt(LocalDateTime.now());
            this.updateById(task);
        } catch (Exception e) {
            log.error("备份任务执行失败 backupId={}", backupId, e);
            markFailed(backupId, "备份执行失败：" + truncate(e.getMessage(), 800));
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (Exception ignored) {
                    // 清理失败不影响状态落库
                }
            }
        } finally {
            releaseLock(lockValue);
        }
    }

    /** 恢复前同步生成安全快照，返回成功态快照任务（含文件元数据） */
    private DbBackupTask createPreRestoreSnapshot(Long sourceBackupId, String reason, Long userId) {
        DbBackupTask snapshot = new DbBackupTask();
        snapshot.setTaskType("PRE_RESTORE_SNAPSHOT");
        snapshot.setStatus("RUNNING");
        snapshot.setFileSize(0L);
        snapshot.setTriggeredBy(userId);
        snapshot.setRelatedBackupId(sourceBackupId);
        snapshot.setRestoreReason(truncate(reason, 500));
        snapshot.setStartedAt(LocalDateTime.now());
        this.save(snapshot);

        Long snapshotId = snapshot.getBackupId();
        String fileName = "pre_restore_" + sourceBackupId + "_" + snapshotId
                + "_" + LocalDateTime.now().format(FILE_TS) + ".sql";
        Path file = Paths.get(backupProperties.getDir()).toAbsolutePath().resolve(fileName);
        try {
            SqlBackupEngine.BackupResult result = backupEngine.backup(
                    file, "pre-restore snapshot of backup_id=" + sourceBackupId);
            snapshot.setStatus("SUCCESS");
            snapshot.setFilePath(file.toString());
            snapshot.setFileSize(result.getFileSize());
            snapshot.setSha256(result.getSha256());
            snapshot.setFinishedAt(LocalDateTime.now());
            this.updateById(snapshot);
            return snapshot;
        } catch (Exception e) {
            markFailed(snapshotId, "恢复前快照失败：" + truncate(e.getMessage(), 800));
            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                    "恢复前安全快照失败，已中止恢复：" + truncate(e.getMessage(), 200));
        }
    }

    private void markFailed(Long backupId, String errorMsg) {
        DbBackupTask update = new DbBackupTask();
        update.setBackupId(backupId);
        update.setStatus("FAILED");
        update.setErrorMsg(truncate(errorMsg, ERROR_MSG_MAX));
        update.setFinishedAt(LocalDateTime.now());
        this.updateById(update);
    }

    // ==================== 锁与审计 ====================

    private boolean acquireLock(String value) {
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeyConstants.BACKUP_LOCK, value, LOCK_TTL);
        return Boolean.TRUE.equals(ok);
    }

    /** 仅允许锁持有者释放（GET 校验后删除） */
    private void releaseLock(String value) {
        try {
            String current = stringRedisTemplate.opsForValue().get(RedisKeyConstants.BACKUP_LOCK);
            if (value.equals(current)) {
                stringRedisTemplate.delete(RedisKeyConstants.BACKUP_LOCK);
            }
        } catch (Exception e) {
            log.warn("备份锁释放异常：{}", e.getMessage());
        }
    }

    private String confirmKey(Long backupId) {
        return RedisKeyConstants.BACKUP_CONFIRM + backupId;
    }

    private void publishAudit(String module, String action, String targetType, String detailJson) {
        Long userId = null;
        try {
            if (SecurityUtils.isAuthenticated()) {
                userId = SecurityUtils.getUserId();
            }
        } catch (Exception ignored) {
            // 无登录上下文留空
        }
        eventPublisher.publishEvent(OperationLogEvent.builder()
                .userId(userId)
                .module(module)
                .action(action)
                .targetType(targetType)
                .ip(ServletUtils.currentIp())
                .deviceId(ServletUtils.currentDeviceId())
                .status(Constants.OP_SUCCESS)
                .detail(detailJson)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private void safePublishAudit(String module, String action, String targetType, String detailJson) {
        try {
            publishAudit(module, action, targetType, detailJson);
        } catch (Exception e) {
            log.error("恢复失败审计落库事件发布异常", e);
        }
    }

    // ==================== 组装 ====================

    private DbBackupTask requireTask(Long backupId) {
        DbBackupTask task = this.getById(backupId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "备份任务不存在：" + backupId);
        }
        return task;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private BackupTaskVO toVO(DbBackupTask entity) {
        BackupTaskVO vo = new BackupTaskVO();
        vo.setBackupId(entity.getBackupId());
        vo.setTaskType(entity.getTaskType());
        vo.setTaskTypeName("PRE_RESTORE_SNAPSHOT".equals(entity.getTaskType()) ? "恢复前快照" : "手工备份");
        vo.setStatus(entity.getStatus());
        vo.setStatusName(switch (entity.getStatus()) {
            case "RUNNING" -> "执行中";
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            default -> entity.getStatus();
        });
        vo.setFilePath(entity.getFilePath());
        vo.setFileSize(entity.getFileSize());
        vo.setSha256(entity.getSha256());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setTriggeredBy(entity.getTriggeredBy());
        vo.setRelatedBackupId(entity.getRelatedBackupId());
        vo.setRestoreReason(entity.getRestoreReason());
        vo.setStartedAt(entity.getStartedAt());
        vo.setFinishedAt(entity.getFinishedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private void fillOperatorNames(List<BackupTaskVO> voList) {
        Set<Long> userIds = voList.stream()
                .map(BackupTaskVO::getTriggeredBy)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = userAccountMapper.selectList(
                        new LambdaQueryWrapper<UserAccount>().in(UserAccount::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(UserAccount::getUserId, UserAccount::getUsername, (a, b) -> a));
        voList.forEach(vo -> vo.setOperatorName(
                vo.getTriggeredBy() == null ? "系统" : nameMap.getOrDefault(vo.getTriggeredBy(), "用户#" + vo.getTriggeredBy())));
    }
}
