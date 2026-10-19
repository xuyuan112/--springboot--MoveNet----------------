-- P7：报告多版本支持 + 训练复核批注
-- 1) 报告同计划同周期需保留历史版本（regenerate 生成新 DRAFT，原版本保留），
--    原 uk_report_period 唯一约束替换为普通索引，新增 version_no 版本号列。
--    注意：uk_report_period 同时是 plan_id 外键的支撑索引，必须先建普通索引再删，
--    否则 MySQL 报 1553（Cannot drop index: needed in a foreign key constraint）。
ALTER TABLE assessment_report
    ADD COLUMN version_no INT NOT NULL DEFAULT 1 COMMENT '报告版本号（同计划同周期内从1递增）' AFTER period_end;
ALTER TABLE assessment_report ADD INDEX idx_report_plan_period (plan_id, period_start, period_end);
ALTER TABLE assessment_report DROP INDEX uk_report_period;

-- 2) 训练复核批注：只增不改，不修改原始评分（P7-B1）
CREATE TABLE rehab_training_annotation (
  annotation_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '批注ID',
  record_id BIGINT UNSIGNED NOT NULL COMMENT '训练记录ID',
  therapist_id BIGINT UNSIGNED NOT NULL COMMENT '批注治疗师用户ID',
  annotation VARCHAR(1000) NOT NULL COMMENT '批注内容（不修改原始评分）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (annotation_id),
  KEY idx_annotation_record (record_id),
  CONSTRAINT fk_annotation_record FOREIGN KEY (record_id) REFERENCES training_record(record_id),
  CONSTRAINT fk_annotation_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练复核批注';
