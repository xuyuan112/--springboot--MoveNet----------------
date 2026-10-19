-- =====================================================================
-- V3：数据库备份恢复任务表（M4 系统运维）
-- 说明：备份采用应用层 JDBC 逻辑备份（不依赖 mysqldump 二进制），
--       备份产物为服务端本地 .sql 文件，任务元数据落本表。
-- =====================================================================

CREATE TABLE db_backup_task (
  backup_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '备份任务ID',
  task_type VARCHAR(20) NOT NULL COMMENT '任务类型：BACKUP 手工备份；PRE_RESTORE_SNAPSHOT 恢复前自动快照',
  status VARCHAR(20) NOT NULL COMMENT '任务状态：RUNNING 执行中 / SUCCESS 成功 / FAILED 失败',
  file_path VARCHAR(500) NULL COMMENT '备份产物绝对路径（.sql）',
  file_size BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '备份文件大小（字节）',
  sha256 CHAR(64) NULL COMMENT '备份文件 SHA-256 摘要',
  error_msg VARCHAR(1000) NULL COMMENT '失败原因（成功为 NULL）',
  triggered_by BIGINT UNSIGNED NULL COMMENT '触发操作人用户ID（系统任务为 NULL）',
  related_backup_id BIGINT UNSIGNED NULL COMMENT '关联备份ID（恢复前快照指向被恢复的备份）',
  restore_reason VARCHAR(500) NULL COMMENT '恢复原因（仅 PRE_RESTORE_SNAPSHOT 记录）',
  started_at DATETIME(3) NOT NULL COMMENT '任务开始时间',
  finished_at DATETIME(3) NULL COMMENT '任务结束时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (backup_id),
  KEY idx_backup_status (status, started_at),
  CONSTRAINT fk_backup_operator FOREIGN KEY (triggered_by) REFERENCES user_account(user_id),
  CONSTRAINT fk_backup_related FOREIGN KEY (related_backup_id) REFERENCES db_backup_task(backup_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库备份恢复任务';
