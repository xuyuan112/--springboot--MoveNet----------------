-- V6: P6 康复计划状态机对齐《接口文档》5.2
-- 旧口径（建表注释）：ACTIVE/COMPLETED/SUSPENDED/CANCELLED
-- 新口径（状态机）：DRAFT → PUBLISHED → PAUSED/COMPLETED/ARCHIVED
--   ACTIVE    → PUBLISHED（已发布，每日调度按该状态生成任务）
--   SUSPENDED → PAUSED   （已暂停）
--   CANCELLED → ARCHIVED （已归档/作废，停止生成新任务）
--   COMPLETED 保持不变（预留给到期自动完成，当前无入口）
-- status 列为 VARCHAR(20) 非 MySQL ENUM，历史值迁移后无残留。

UPDATE rehab_plan SET status = 'PUBLISHED' WHERE status = 'ACTIVE';
UPDATE rehab_plan SET status = 'PAUSED'    WHERE status = 'SUSPENDED';
UPDATE rehab_plan SET status = 'ARCHIVED'  WHERE status = 'CANCELLED';

ALTER TABLE rehab_plan
  MODIFY COLUMN status VARCHAR(20) NOT NULL
  COMMENT '状态：DRAFT草稿/PUBLISHED已发布/PAUSED已暂停/COMPLETED已完成/ARCHIVED已归档';
