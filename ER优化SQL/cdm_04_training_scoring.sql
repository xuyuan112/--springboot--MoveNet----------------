-- ============================================================
-- CDM-04 训练执行与姿态评分（Training & Scoring）
-- 用途：PowerDesigner 反向工程 -> 生成 CDM-04 PDM / CDM
-- 依据：er图分模块建议.md 模块四
--
-- 本模块核心 2 张表：
--   training_record      训练记录（含 action_version_id 快照）
--   training_error_log   训练纠错日志
--
-- 外部引用实体 3 张（图上用浅色/虚线标识）：
--   plan_daily_task      每日训练任务（见 CDM-03）
--   action_version       动作版本（见 CDM-02）
--   action_rule          动作评估规则（见 CDM-02）
--
-- 跨模块外部引用（字段保留，不建物理外键）：
--   plan_daily_task.plan_item_id -> plan_item（见 CDM-03）
--
-- 本模块内部闭环的关键关系：
--   training_record.daily_task_id   -> plan_daily_task.task_id
--   training_record.action_version_id -> action_version.action_version_id（快照）
--   training_error_log.record_id    -> training_record.record_id
--   training_error_log.rule_id      -> action_rule.action_rule_id
--   action_rule.action_version_id   -> action_version.action_version_id（保留，两表均在本文件）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ---------- 1. 每日训练任务（外部引用实体，见 CDM-03） ----------
-- plan_item_id 指向 plan_item（CDM-03），本文件不建外键
CREATE TABLE plan_daily_task (
    task_id BIGINT NOT NULL COMMENT '主键ID',
    plan_item_id BIGINT NOT NULL COMMENT '计划动作项ID（外部引用 plan_item，见 CDM-03）',
    task_date DATE NOT NULL COMMENT '任务日期',
    target_sets INT NOT NULL COMMENT '目标组数',
    target_reps INT NULL COMMENT '目标次数',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    skip_reason VARCHAR(255) NULL COMMENT '跳过原因',
    started_at DATETIME NULL COMMENT '训练开始时间',
    completed_at DATETIME NULL COMMENT '完成时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (task_id),
    UNIQUE KEY uk_task_item_date (plan_item_id,task_date),
    KEY idx_task_date_status (task_date,status)
) COMMENT='每日训练任务';

-- ---------- 2. 动作版本（外部引用实体，见 CDM-02） ----------
CREATE TABLE action_version (
    action_version_id BIGINT NOT NULL COMMENT '主键ID',
    action_id BIGINT NOT NULL COMMENT '标准动作ID（外部引用 action，见 CDM-02）',
    version_no INT NOT NULL COMMENT '版本号',
    action_type VARCHAR(20) NOT NULL COMMENT '动作类型',
    algo_config JSON NOT NULL COMMENT '算法参数快照',
    motion_signature JSON NOT NULL COMMENT '动作签名',
    pose_engine_id VARCHAR(150) NOT NULL COMMENT '姿态引擎版本',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    created_by BIGINT NOT NULL COMMENT '创建人ID（外部引用 user_account，见 CDM-01）',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (action_version_id),
    UNIQUE KEY uk_action_version (action_id,version_no)
) COMMENT='动作版本';

-- ---------- 3. 动作评估规则（外部引用实体，见 CDM-02） ----------
-- action_version_id 和自关联均指向本文件内的 action_version / action_rule，保留外键
CREATE TABLE action_rule (
    action_rule_id BIGINT NOT NULL COMMENT '主键ID',
    action_version_id BIGINT NOT NULL COMMENT '动作版本ID',
    joint_name VARCHAR(50) NOT NULL COMMENT '关节名称',
    keypoint_indices JSON NOT NULL COMMENT '关键点索引',
    angle_formula VARCHAR(100) NOT NULL COMMENT '角度计算公式',
    joint_role VARCHAR(20) NOT NULL COMMENT '关节角色',
    dimension VARCHAR(20) NOT NULL COMMENT '评分维度',
    phase VARCHAR(20) NOT NULL COMMENT '动作阶段',
    min_angle DECIMAL(6,2) NULL,
    max_angle DECIMAL(6,2) NULL,
    standard_angle DECIMAL(6,2) NULL,
    tolerance_soft DECIMAL(6,2) NULL,
    tolerance_hard DECIMAL(6,2) NULL,
    hold_frames_required INT NULL,
    speed_min DECIMAL(6,2) NULL,
    speed_max DECIMAL(6,2) NULL,
    score_weight DECIMAL(5,4) NOT NULL,
    severity TINYINT NOT NULL,
    prompt_text VARCHAR(255) NOT NULL,
    symmetry_pair_rule_id BIGINT NULL COMMENT '对称配对规则ID（必须属于同一动作版本）',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (action_rule_id),
    UNIQUE KEY uk_rule_dimension (action_version_id,joint_name,dimension),
    UNIQUE KEY uk_rule_id_version (action_rule_id,action_version_id),
    CONSTRAINT fk_rule_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
    CONSTRAINT fk_rule_pair FOREIGN KEY (symmetry_pair_rule_id,action_version_id)
        REFERENCES action_rule(action_rule_id,action_version_id)
) COMMENT='动作评估规则';

-- ---------- 4. 训练记录（本模块核心，含 action_version_id 快照） ----------
CREATE TABLE training_record (
    record_id BIGINT NOT NULL COMMENT '主键ID',
    daily_task_id BIGINT NOT NULL COMMENT '每日任务ID',
    action_version_id BIGINT NOT NULL COMMENT '动作版本快照（提交时复制自 plan_item）',
    started_at DATETIME NOT NULL COMMENT '训练开始时间',
    ended_at DATETIME NOT NULL COMMENT '训练结束时间',
    total_reps INT NOT NULL COMMENT '总次数',
    correct_reps INT NOT NULL COMMENT '达标次数',
    total_score DECIMAL(5,2) NOT NULL COMMENT '训练总评分',
    score_detail JSON NOT NULL COMMENT '分项评分快照',
    avg_rom DECIMAL(6,2) NULL,
    peak_angle DECIMAL(6,2) NULL,
    trough_angle DECIMAL(6,2) NULL,
    peak_frame_no INT NULL,
    trough_frame_no INT NULL,
    hold_ratio DECIMAL(5,4) NULL,
    template_similarity DECIMAL(5,4) NULL,
    valid_frame_ratio DECIMAL(5,4) NULL,
    pose_repaired_ratio DECIMAL(5,4) NULL,
    stability_score DECIMAL(5,2) NULL,
    symmetry_score DECIMAL(5,2) NULL,
    pose_quality VARCHAR(20) NOT NULL COMMENT '姿态质量',
    pose_engine_id VARCHAR(150) NOT NULL COMMENT '姿态引擎版本',
    client_request_id VARCHAR(100) NOT NULL COMMENT '客户端幂等请求ID',
    remark TEXT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_record_idempotency (daily_task_id,client_request_id),
    KEY idx_record_task_time (daily_task_id,created_at),
    KEY idx_record_version_time (action_version_id,created_at),
    CONSTRAINT fk_record_task FOREIGN KEY (daily_task_id) REFERENCES plan_daily_task(task_id),
    CONSTRAINT fk_record_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
    CONSTRAINT chk_record_time CHECK (ended_at >= started_at),
    CONSTRAINT chk_record_reps CHECK (total_reps >= 0 AND correct_reps BETWEEN 0 AND total_reps),
    CONSTRAINT chk_record_score CHECK (total_score BETWEEN 0 AND 100)
) COMMENT='训练记录';

-- ---------- 5. 训练纠错日志 ----------
CREATE TABLE training_error_log (
    error_id BIGINT NOT NULL COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '训练记录ID',
    rule_id BIGINT NULL COMMENT '动作规则ID',
    dimension VARCHAR(20) NOT NULL COMMENT '评分维度',
    phase VARCHAR(20) NOT NULL COMMENT '动作阶段',
    error_type VARCHAR(40) NOT NULL COMMENT '错误类型',
    joint_name VARCHAR(50) NULL,
    current_value DECIMAL(8,3) NULL,
    expected_value DECIMAL(8,3) NULL,
    deviation DECIMAL(8,3) NULL,
    severity TINYINT NOT NULL,
    frame_no INT NULL,
    prompt_text VARCHAR(255) NOT NULL,
    error_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (error_id),
    KEY idx_error_record (record_id),
    KEY idx_error_dimension (dimension,severity),
    CONSTRAINT fk_error_record FOREIGN KEY (record_id) REFERENCES training_record(record_id),
    CONSTRAINT fk_error_rule FOREIGN KEY (rule_id) REFERENCES action_rule(action_rule_id)
) COMMENT='训练纠错日志';

-- ---------- 自增主键 ----------
ALTER TABLE plan_daily_task MODIFY task_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_version MODIFY action_version_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_rule MODIFY action_rule_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE training_record MODIFY record_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE training_error_log MODIFY error_id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS=1;
