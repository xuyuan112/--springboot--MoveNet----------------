-- ============================================================
-- CDM-00 系统总览（Overview）
-- 用途：PowerDesigner 反向工程 -> 生成总览 PDM / CDM
-- 依据：er图分模块建议.md 第 39 行"总览图只画 10~12 个核心实体"
--
-- 包含 12 张核心表：
--   主链路：user_account -> patient_therapist -> rehab_plan
--           -> plan_item -> plan_daily_task -> training_record -> assessment_report
--   跨模块接入：action_version / content / consultation / file_object / operation_log
--
-- 跨模块外部引用（字段保留，不建物理外键，避免反向工程报缺失表）：
--   action_version.action_id      -> action            (见 CDM-02)
--   rehab_plan.rehab_part_code    -> rehab_part_dict   (见 CDM-02 / CDM-06)
--   content.target_part_code      -> rehab_part_dict   (见 CDM-06)
--
-- 本文件不包含：patient_profile / therapist_profile / patient_rehab_part /
--   action / action_rule / action_template / action_template_sample /
--   rehab_part_dict / plan_schedule / training_error_log /
--   content_attachment / content_collection /
--   consultation_message / consultation_attachment / announcement
--   （这些在各自子模块 CDM-01~07 中展开）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ---------- 1. 用户账号（所有外键的根） ----------
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
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_phone (phone),
    CONSTRAINT chk_user_role CHECK (role_code IN ('PATIENT','THERAPIST','ADMIN')),
    CONSTRAINT chk_user_status CHECK (status IN (0,1)),
    CONSTRAINT chk_user_deleted CHECK (deleted IN (0,1))
) COMMENT='用户账号';

-- ---------- 2. 患者-治疗师绑定 ----------
CREATE TABLE patient_therapist (
    binding_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID',
    therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID',
    relation_type VARCHAR(20) NOT NULL COMMENT '关系类型',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    start_at DATETIME NOT NULL COMMENT '生效时间',
    end_at DATETIME NULL COMMENT '结束时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (binding_id),
    -- 复合唯一键，供 rehab_plan / consultation 的 binding_id 复合外键引用
    UNIQUE KEY uk_binding_id_pair (binding_id, patient_id, therapist_id),
    KEY idx_binding_patient (patient_id,status),
    KEY idx_binding_therapist (therapist_id,status),
    CONSTRAINT fk_binding_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_binding_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_binding_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id),
    CONSTRAINT chk_binding_period CHECK (end_at IS NULL OR end_at >= start_at)
) COMMENT='患者治疗师关系';

-- ---------- 3. 动作版本（算法域接入点；action_id 为外部引用，不建外键） ----------
CREATE TABLE action_version (
    action_version_id BIGINT NOT NULL COMMENT '主键ID',
    action_id BIGINT NOT NULL COMMENT '标准动作ID（外部引用 action，见 CDM-02）',
    version_no INT NOT NULL COMMENT '版本号',
    action_type VARCHAR(20) NOT NULL COMMENT '动作类型',
    algo_config JSON NOT NULL COMMENT '算法参数快照',
    motion_signature JSON NOT NULL COMMENT '动作签名',
    pose_engine_id VARCHAR(150) NOT NULL COMMENT '姿态引擎版本',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (action_version_id),
    UNIQUE KEY uk_action_version (action_id,version_no),
    KEY idx_action_version_status (action_id,status),
    CONSTRAINT fk_av_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id)
) COMMENT='动作版本';

-- ---------- 4. 康复计划（binding_id 内联；rehab_part_code 外部引用，不建外键） ----------
CREATE TABLE rehab_plan (
    plan_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID',
    therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID',
    binding_id BIGINT NOT NULL COMMENT '患者治疗师绑定ID',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    rehab_part_code VARCHAR(30) NOT NULL COMMENT '康复部位编码（外部引用 rehab_part_dict，见 CDM-02/06）',
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
    -- 复合外键：保证 patient_id/therapist_id 与绑定记录一致
    CONSTRAINT fk_plan_binding_pair FOREIGN KEY (binding_id, patient_id, therapist_id)
        REFERENCES patient_therapist (binding_id, patient_id, therapist_id),
    CONSTRAINT chk_plan_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_plan_version CHECK (version_no > 0)
) COMMENT='康复计划';

-- ---------- 5. 计划动作项 ----------
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

-- ---------- 7. 训练记录（含 action_version_id 快照） ----------
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
    CONSTRAINT fk_record_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
    CONSTRAINT chk_record_time CHECK (ended_at >= started_at),
    CONSTRAINT chk_record_reps CHECK (total_reps >= 0 AND correct_reps BETWEEN 0 AND total_reps),
    CONSTRAINT chk_record_score CHECK (total_score BETWEEN 0 AND 100)
) COMMENT='训练记录';

-- ---------- 8. 评估报告 ----------
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

-- ---------- 9. 公共内容（target_part_code 外部引用，不建外键） ----------
CREATE TABLE content (
    content_id BIGINT NOT NULL COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content_type VARCHAR(30) NOT NULL COMMENT '内容类型',
    target_part_code VARCHAR(30) NOT NULL COMMENT '目标部位编码（外部引用 rehab_part_dict，见 CDM-06）',
    target_group VARCHAR(100) NULL COMMENT '适用人群',
    summary VARCHAR(500) NULL COMMENT '内容摘要',
    content_body LONGTEXT NOT NULL COMMENT '内容正文',
    author_id BIGINT NOT NULL COMMENT '内容作者ID',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    reviewer_id BIGINT NULL COMMENT '审核人ID',
    review_remark VARCHAR(500) NULL COMMENT '审核意见',
    valid_from DATETIME NOT NULL COMMENT '有效开始时间',
    valid_to DATETIME NULL COMMENT '有效结束时间',
    published_at DATETIME NULL COMMENT '发布时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (content_id),
    KEY idx_content_filter (status,target_part_code,valid_from,valid_to),
    CONSTRAINT fk_content_author FOREIGN KEY (author_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_content_reviewer FOREIGN KEY (reviewer_id) REFERENCES user_account(user_id)
) COMMENT='公共健康内容';

-- ---------- 10. 咨询会话（binding_id 内联） ----------
CREATE TABLE consultation (
    consultation_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID',
    therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID',
    binding_id BIGINT NOT NULL COMMENT '患者治疗师绑定ID',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    closed_at DATETIME NULL COMMENT '关闭时间',
    PRIMARY KEY (consultation_id),
    KEY idx_consult_patient (patient_id,status),
    KEY idx_consult_therapist (therapist_id,status),
    KEY idx_consult_binding (binding_id),
    CONSTRAINT fk_consult_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_consult_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_consult_binding_pair FOREIGN KEY (binding_id, patient_id, therapist_id)
        REFERENCES patient_therapist (binding_id, patient_id, therapist_id)
) COMMENT='咨询会话';

-- ---------- 11. 文件对象（共享实体） ----------
CREATE TABLE file_object (
    file_id BIGINT NOT NULL COMMENT '主键ID',
    bucket_name VARCHAR(100) NOT NULL COMMENT 'MinIO存储桶',
    object_key VARCHAR(500) NOT NULL COMMENT 'MinIO对象键',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    content_type VARCHAR(100) NOT NULL COMMENT '内容/文件类型',
    file_size BIGINT NOT NULL COMMENT '文件大小字节',
    sha256 CHAR(64) NOT NULL COMMENT 'SHA-256哈希',
    purpose VARCHAR(30) NOT NULL COMMENT '文件用途',
    scan_status VARCHAR(20) NOT NULL COMMENT '安全扫描状态',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    PRIMARY KEY (file_id),
    UNIQUE KEY uk_file_bucket_object (bucket_name,object_key),
    KEY idx_file_purpose (purpose,scan_status),
    CONSTRAINT fk_file_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id)
) COMMENT='MinIO文件对象';

-- ---------- 12. 操作审计日志 ----------
CREATE TABLE operation_log (
    log_id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NULL COMMENT '用户账号ID',
    module VARCHAR(30) NOT NULL COMMENT '业务模块',
    action VARCHAR(50) NOT NULL COMMENT '操作动作',
    target_type VARCHAR(50) NOT NULL COMMENT '目标类型',
    target_id BIGINT NULL COMMENT '目标对象ID（多态，不建外键）',
    ip VARCHAR(45) NOT NULL COMMENT '客户端IP',
    device_id VARCHAR(100) NULL COMMENT '设备标识',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    detail JSON NULL COMMENT '操作详情',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (log_id),
    KEY idx_log_user_time (user_id,created_at),
    KEY idx_log_module_time (module,created_at),
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES user_account(user_id)
) COMMENT='操作审计日志';

-- ---------- 自增主键 ----------
ALTER TABLE user_account MODIFY user_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE patient_therapist MODIFY binding_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_version MODIFY action_version_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE rehab_plan MODIFY plan_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE plan_item MODIFY plan_item_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE plan_daily_task MODIFY task_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE training_record MODIFY record_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE assessment_report MODIFY report_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE content MODIFY content_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE consultation MODIFY consultation_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE file_object MODIFY file_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE operation_log MODIFY log_id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS=1;
