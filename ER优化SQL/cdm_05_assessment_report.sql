-- ============================================================
-- CDM-05 评估报告与训练分析（Assessment & Analysis）
-- 用途：PowerDesigner 反向工程 -> 生成 CDM-05 PDM / CDM
-- 依据：er图分模块建议.md 模块五
--
-- 本模块核心 1 张表：
--   assessment_report     评估报告
--
-- 外部引用实体 4 张（图上用浅色/虚线标识）：
--   user_account         用户账号（见 CDM-01）
--   rehab_plan            康复计划（见 CDM-03）
--   plan_daily_task       每日训练任务（见 CDM-04）
--   training_record      训练记录（见 CDM-04）
--
-- 跨模块外部引用（字段保留，不建物理外键）：
--   rehab_plan.binding_id     -> patient_therapist（见 CDM-01）
--   rehab_plan.rehab_part_code -> rehab_part_dict（见 CDM-02）
--   plan_daily_task.plan_item_id -> plan_item（见 CDM-03）
--   training_record.action_version_id -> action_version（见 CDM-02）
--
-- 本模块内部闭环的关键关系：
--   assessment_report.plan_id       -> rehab_plan.plan_id
--   assessment_report.reviewed_by   -> user_account.user_id（审核人）
--   rehab_plan.patient_id/therapist_id -> user_account.user_id（保留）
--   training_record.daily_task_id   -> plan_daily_task.task_id
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ---------- 1. 用户账号（外部引用实体，见 CDM-01） ----------
CREATE TABLE user_account (
    user_id BIGINT NOT NULL COMMENT '用户账号ID',
    username VARCHAR(50) NOT NULL COMMENT '登录名',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt密码密文',
    role_code VARCHAR(20) NOT NULL COMMENT '角色：PATIENT/THERAPIST/ADMIN',
    phone VARCHAR(20) NULL COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用0禁用',
    last_login_at DATETIME NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (user_id)
) COMMENT='用户账号';

-- ---------- 2. 康复计划（外部引用实体，见 CDM-03） ----------
-- binding_id 指向 patient_therapist（CDM-01），rehab_part_code 指向 rehab_part_dict（CDM-02），本文件不建外键
-- patient_id/therapist_id 指向 user_account（本文件内），保留外键
CREATE TABLE rehab_plan (
    plan_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID',
    therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID',
    binding_id BIGINT NOT NULL COMMENT '患者治疗师绑定ID（外部引用 patient_therapist，见 CDM-01）',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    rehab_part_code VARCHAR(30) NOT NULL COMMENT '康复部位编码（外部引用 rehab_part_dict，见 CDM-02）',
    version_no INT NOT NULL COMMENT '版本号',
    start_date DATE NOT NULL COMMENT '计划开始日期',
    end_date DATE NOT NULL COMMENT '计划结束日期',
    frequency_type VARCHAR(20) NOT NULL COMMENT '频率类型',
    frequency_value INT NULL COMMENT '频率值',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    remark TEXT NULL COMMENT '备注',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (plan_id),
    UNIQUE KEY uk_plan_version (patient_id,name,version_no),
    KEY idx_plan_patient_status (patient_id,status),
    KEY idx_plan_binding (binding_id),
    CONSTRAINT fk_plan_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_plan_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id),
    CONSTRAINT chk_plan_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_plan_version CHECK (version_no > 0)
) COMMENT='康复计划';

-- ---------- 3. 每日训练任务（外部引用实体，见 CDM-04） ----------
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
    KEY idx_task_date_status (task_date,status),
    CONSTRAINT chk_task_target_sets CHECK (target_sets > 0),
    CONSTRAINT chk_task_target_reps CHECK (target_reps IS NULL OR target_reps >= 0)
) COMMENT='每日训练任务';

-- ---------- 4. 训练记录（外部引用实体，见 CDM-04，字段与主表全量对齐） ----------
-- daily_task_id 指向 plan_daily_task（本文件内），保留外键
-- action_version_id 指向 action_version（CDM-02），本文件不建外键
CREATE TABLE training_record (
    record_id BIGINT NOT NULL COMMENT '主键ID',
    daily_task_id BIGINT NOT NULL COMMENT '每日任务ID',
    action_version_id BIGINT NOT NULL COMMENT '动作版本快照（提交时从 plan_item 复制，不随后续版本变更变化）',
    started_at DATETIME NOT NULL COMMENT '训练开始时间',
    ended_at DATETIME NOT NULL COMMENT '训练结束时间',
    total_reps INT NOT NULL COMMENT '总次数',
    correct_reps INT NOT NULL COMMENT '达标次数',
    total_score DECIMAL(5,2) NOT NULL COMMENT '训练总评分',
    score_detail JSON NOT NULL COMMENT '分项评分快照',
    avg_rom DECIMAL(6,2) NULL COMMENT '平均关节活动度',
    peak_angle DECIMAL(6,2) NULL COMMENT '峰值角度',
    trough_angle DECIMAL(6,2) NULL COMMENT '谷值角度',
    peak_frame_no INT NULL COMMENT '峰值帧号',
    trough_frame_no INT NULL COMMENT '谷值帧号',
    hold_ratio DECIMAL(5,4) NULL COMMENT '保持比例',
    template_similarity DECIMAL(5,4) NULL COMMENT '模板相似度',
    valid_frame_ratio DECIMAL(5,4) NULL COMMENT '有效帧比例',
    pose_repaired_ratio DECIMAL(5,4) NULL COMMENT '姿态修复比例',
    stability_score DECIMAL(5,2) NULL COMMENT '稳定性评分',
    symmetry_score DECIMAL(5,2) NULL COMMENT '对称性评分',
    pose_quality VARCHAR(20) NOT NULL COMMENT '姿态质量',
    pose_engine_id VARCHAR(150) NOT NULL COMMENT '姿态引擎版本',
    client_request_id VARCHAR(100) NOT NULL COMMENT '客户端幂等请求ID',
    remark TEXT NULL COMMENT '备注',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_record_idempotency (daily_task_id,client_request_id),
    KEY idx_record_task_time (daily_task_id,created_at),
    KEY idx_record_version_time (action_version_id,created_at),
    CONSTRAINT fk_record_task FOREIGN KEY (daily_task_id) REFERENCES plan_daily_task(task_id),
    CONSTRAINT chk_record_time CHECK (ended_at >= started_at),
    CONSTRAINT chk_record_reps CHECK (total_reps >= 0 AND correct_reps BETWEEN 0 AND total_reps),
    CONSTRAINT chk_record_score CHECK (total_score BETWEEN 0 AND 100)
) COMMENT='训练记录';

-- ---------- 5. 评估报告（本模块核心，字段与主表全量对齐） ----------
CREATE TABLE assessment_report (
    report_id BIGINT NOT NULL COMMENT '主键ID',
    plan_id BIGINT NOT NULL COMMENT '康复计划ID',
    period_start DATE NOT NULL COMMENT '报告周期开始',
    period_end DATE NOT NULL COMMENT '报告周期结束',
    data_snapshot JSON NOT NULL COMMENT '报告数据快照',
    ai_draft TEXT NULL COMMENT 'AI辅助草稿',
    therapist_conclusion TEXT NULL COMMENT '治疗师结论',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    reviewed_by BIGINT NULL COMMENT '审核人ID',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (report_id),
    UNIQUE KEY uk_report_period (plan_id,period_start,period_end),
    CONSTRAINT fk_report_plan FOREIGN KEY (plan_id) REFERENCES rehab_plan(plan_id),
    CONSTRAINT fk_report_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account(user_id),
    CONSTRAINT chk_report_period CHECK (period_end >= period_start)
) COMMENT='评估报告';

-- ---------- 自增主键 ----------
ALTER TABLE user_account MODIFY user_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE rehab_plan MODIFY plan_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE plan_daily_task MODIFY task_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE training_record MODIFY record_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE assessment_report MODIFY report_id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS=1;
