-- V5: P4-B2 训练记录撤回（软删除）支撑字段
-- 撤回语义：患者可撤回尚未纳入报告的训练记录（软删除 + 原因）；
-- 已纳入报告（assessment_report 统计周期覆盖该记录日期）的记录不可撤回（409）。

ALTER TABLE training_record
  ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已撤回' AFTER created_at,
  ADD COLUMN withdraw_reason VARCHAR(255) NULL COMMENT '撤回原因（撤回必填）' AFTER deleted,
  ADD COLUMN withdrawn_at DATETIME(3) NULL COMMENT '撤回时间' AFTER withdraw_reason;

-- 撤回记录常按时间排查，补索引便于审计过滤（逻辑删除过滤由查询层完成）
CREATE INDEX idx_record_withdrawn ON training_record (deleted, withdrawn_at);
