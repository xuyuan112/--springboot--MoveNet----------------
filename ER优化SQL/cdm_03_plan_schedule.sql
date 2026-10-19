-- ============================================================
-- CDM-03 康复计划与任务调度（Plan & Schedule）
-- 用途：PowerDesigner 反向工程 -> 生成 CDM-03 PDM / CDM
-- 依据：er图分模块建议.md 模块三
--
-- 本模块核心 4 张表：
--   rehab_plan          康复计划
--   plan_item           计划动作项
--   plan_schedule       计划执行日程
--   plan_daily_task     每日训练任务
--
-- 外部引用实体 2 张（在图上用浅色/虚线标识）：
--   patient_therapist   患者-治疗师绑定（见 CDM-01）
--   action_version      动作版本（见 CDM-02）
--
-- 跨模块外部引用（字段保留，不建物理外键）：
--   rehab_plan.patient_id     -> user_account（见 CDM-01）
--   rehab_plan.therapist_id   -> user_account（见 CDM-01）
--   rehab_plan.rehab_part_code -> rehab_part_dict（见 CDM-02）
--   patient_therapist.patient_id/therapist_id/created_by -> user_account（见 CDM-01）
--   action_version.action_id  -> action（见 CDM-02）
--   action_version.created_by -> user_account（见 CDM-01）
--
-- 不包含：user_account、action、rehab_part_dict（见 CDM-01/02）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ---------- 1. 患者-治疗师绑定（外部引用实体，见 CDM-01） ----------
-- 本文件仅保留 rehab_plan 复合外键所需的最小结构：id/patient_id/therapist_id + uk_binding_id_pair
CREATE TABLE patient_therapist (
    binding_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID（外部引用 user_account，见 CDM-01）',
    therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID（外部引用 user_account，见 CDM-01）',
    relation_type VARCHAR(20) NOT NULL COMMENT '关系类型',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    start_at DATETIME NOT NULL COMMENT '生效时间',
    end_at DATETIME NULL COMMENT '结束时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID（外部引用 user_account，见 CDM-01）',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (binding_id),
    UNIQUE KEY uk_binding_id_pair (binding_id, patient_id, therapist_id)
) COMMENT='患者治疗师关系';

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

-- ---------- 3. 康复计划（本模块核心） ----------
CREATE TABLE rehab_plan (
    plan_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID（外部引用 user_account，见 CDM-01）',
    therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID（外部引用 user_account，见 CDM-01）',
    binding_id BIGINT NOT NULL COMMENT '患者治疗师绑定ID',
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
    -- 复合外键：保证 patient_id/therapist_id 与绑定记录一致
    CONSTRAINT fk_plan_binding_pair FOREIGN KEY (binding_id, patient_id, therapist_id)
        REFERENCES patient_therapist (binding_id, patient_id, therapist_id),
    CONSTRAINT chk_plan_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_plan_version CHECK (version_no > 0)
) COMMENT='康复计划';

-- ---------- 4. 计划动作项 ----------
CREATE TABLE plan_item (
    plan_item_id BIGINT NOT NULL COMMENT '主键ID',
    plan_id BIGINT NOT NULL COMMENT '康复计划ID',
    action_version_id BIGINT NOT NULL COMMENT '动作版本ID',
    sequence_no INT NOT NULL COMMENT '动作顺序号',
    target_sets INT NOT NULL COMMENT '目标组数',
    target_reps INT NULL COMMENT '目标次数',
    target_amplitude DECIMAL(6,2) NULL COMMENT '目标幅度',
    duration_sec INT NULL COMMENT '持续时长秒',
    rest_sec INT NULL COMMENT '休息时长秒',
    instruction TEXT NULL COMMENT '动作指导说明',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (plan_item_id),
    UNIQUE KEY uk_plan_item_sequence (plan_id,sequence_no),
    CONSTRAINT fk_item_plan FOREIGN KEY (plan_id) REFERENCES rehab_plan(plan_id),
    CONSTRAINT fk_item_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
    CONSTRAINT chk_item_sequence CHECK (sequence_no > 0),
    CONSTRAINT chk_item_sets CHECK (target_sets > 0),
    CONSTRAINT chk_item_reps CHECK (target_reps IS NULL OR target_reps >= 0),
    CONSTRAINT chk_item_duration CHECK (duration_sec IS NULL OR duration_sec >= 0),
    CONSTRAINT chk_item_rest CHECK (rest_sec IS NULL OR rest_sec >= 0)
) COMMENT='计划动作项';

-- ---------- 5. 计划执行日程 ----------
CREATE TABLE plan_schedule (
    schedule_id BIGINT NOT NULL COMMENT '主键ID',
    plan_id BIGINT NOT NULL COMMENT '康复计划ID',
    weekday TINYINT NOT NULL COMMENT '执行星期1至7',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (schedule_id),
    UNIQUE KEY uk_schedule_day (plan_id,weekday),
    CONSTRAINT fk_schedule_plan FOREIGN KEY (plan_id) REFERENCES rehab_plan(plan_id),
    CONSTRAINT chk_schedule_weekday CHECK (weekday BETWEEN 1 AND 7)
) COMMENT='计划执行日程';

-- ---------- 6. 每日训练任务 ----------
CREATE TABLE plan_daily_task (
    task_id BIGINT NOT NULL COMMENT '主键ID',
    plan_item_id BIGINT NOT NULL COMMENT '计划动作项ID',
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
    CONSTRAINT fk_task_item FOREIGN KEY (plan_item_id) REFERENCES plan_item(plan_item_id),
    CONSTRAINT chk_task_target_sets CHECK (target_sets > 0),
    CONSTRAINT chk_task_target_reps CHECK (target_reps IS NULL OR target_reps >= 0)
) COMMENT='每日训练任务';

-- ---------- 自增主键 ----------
ALTER TABLE patient_therapist MODIFY binding_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_version MODIFY action_version_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE rehab_plan MODIFY plan_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE plan_item MODIFY plan_item_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE plan_schedule MODIFY schedule_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE plan_daily_task MODIFY task_id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS=1;
