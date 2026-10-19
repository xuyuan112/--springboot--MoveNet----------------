-- PowerDesigner逆向工程专用 MySQL DDL
-- 外键约束已全部移入CREATE TABLE内部，确保PowerDesigner正确识别
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ============================================
-- 模块一：用户与权限
-- ============================================

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

CREATE TABLE rehab_part_dict (
    code VARCHAR(30) NOT NULL COMMENT '康复部位编码',
    name VARCHAR(50) NOT NULL COMMENT '康复部位名称',
    group_code VARCHAR(20) NOT NULL COMMENT '部位大类：UPPER上肢/LOWER下肢/SPINE脊柱/OVERALL全身综合',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用标记',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (code),
    UNIQUE KEY uk_part_name (name)
) COMMENT='康复部位字典';

CREATE TABLE patient_profile (
    patient_profile_id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户账号ID',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    gender CHAR(1) NOT NULL COMMENT '性别',
    birth_date DATE NOT NULL COMMENT '出生日期',
    height_cm DECIMAL(5,2) NULL COMMENT '身高厘米',
    weight_kg DECIMAL(5,2) NULL COMMENT '体重千克',
    medical_history TEXT NULL COMMENT '病史摘要',
    allergy_info TEXT NULL COMMENT '过敏信息',
    contraindication TEXT NULL COMMENT '禁忌说明',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (patient_profile_id),
    UNIQUE KEY uk_patient_user (user_id),
    CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES user_account(user_id),
    CONSTRAINT chk_patient_gender CHECK (gender IN ('M','F','U')),
    CONSTRAINT chk_patient_height CHECK (height_cm IS NULL OR height_cm > 0),
    CONSTRAINT chk_patient_weight CHECK (weight_kg IS NULL OR weight_kg > 0)
) COMMENT='患者档案';

CREATE TABLE patient_rehab_part (
    patient_rehab_part_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID(user_account.id)',
    rehab_part_code VARCHAR(30) NOT NULL COMMENT '康复部位编码',
    side VARCHAR(10) NOT NULL DEFAULT 'BILATERAL' COMMENT '侧别 LEFT/RIGHT/BILATERAL',
    primary_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否主康复部位 1是0否',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RESOLVED',
    diagnosed_at DATE NULL COMMENT '确诊或受损日期',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (patient_rehab_part_id),
    KEY idx_pp_patient_status (patient_id,status),
    KEY idx_pp_part_side (patient_id,rehab_part_code,side),
    CONSTRAINT fk_pp_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_pp_part FOREIGN KEY (rehab_part_code) REFERENCES rehab_part_dict(code),
    CONSTRAINT chk_pp_side CHECK (side IN ('LEFT','RIGHT','BILATERAL')),
    CONSTRAINT chk_pp_primary CHECK (primary_flag IN (0,1))
) COMMENT='患者康复部位（多对多）';

CREATE TABLE therapist_profile (
    therapist_profile_id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户账号ID',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    title VARCHAR(50) NOT NULL COMMENT '职称',
    specialty VARCHAR(100) NOT NULL COMMENT '专业特长',
    license_no VARCHAR(100) NULL COMMENT '执业资质编号',
    introduction TEXT NULL COMMENT '个人简介',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (therapist_profile_id),
    UNIQUE KEY uk_therapist_user (user_id),
    UNIQUE KEY uk_therapist_license (license_no),
    CONSTRAINT fk_therapist_user FOREIGN KEY (user_id) REFERENCES user_account(user_id)
) COMMENT='治疗师档案';

CREATE TABLE patient_therapist (
    -- patient_id/therapist_id 使用 user_account ID；必须由应用层校验对应角色及有效绑定关系
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
    KEY idx_binding_patient (patient_id,status),
    KEY idx_binding_therapist (therapist_id,status),
    CONSTRAINT fk_binding_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_binding_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_binding_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id),
    CONSTRAINT chk_binding_period CHECK (end_at IS NULL OR end_at >= start_at)
) COMMENT='患者治疗师关系';

-- ============================================
-- 模块二：动作与算法
-- ============================================

CREATE TABLE action (
    action_id BIGINT NOT NULL COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    target_part_code VARCHAR(30) NOT NULL COMMENT '目标部位编码',
    difficulty VARCHAR(20) NOT NULL COMMENT '动作难度',
    description TEXT NULL COMMENT '动作描述',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (action_id),
    KEY idx_action_part_status (target_part_code,status),
    CONSTRAINT fk_action_part FOREIGN KEY (target_part_code) REFERENCES rehab_part_dict(code),
    CONSTRAINT fk_action_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id),
    CONSTRAINT chk_action_deleted CHECK (deleted IN (0,1))
) COMMENT='标准动作';

CREATE TABLE action_version (
    action_version_id BIGINT NOT NULL COMMENT '主键ID',
    action_id BIGINT NOT NULL COMMENT '标准动作ID',
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
    CONSTRAINT fk_av_action FOREIGN KEY (action_id) REFERENCES action(action_id),
    CONSTRAINT fk_av_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id)
) COMMENT='动作版本';

CREATE TABLE action_rule (
    action_rule_id BIGINT NOT NULL COMMENT '主键ID',
    action_version_id BIGINT NOT NULL COMMENT '动作版本ID',
    joint_name VARCHAR(50) NOT NULL COMMENT '关节名称',
    keypoint_indices JSON NOT NULL COMMENT '关键点索引',
    angle_formula VARCHAR(100) NOT NULL COMMENT '角度计算公式',
    joint_role VARCHAR(20) NOT NULL COMMENT '关节角色',
    dimension VARCHAR(20) NOT NULL COMMENT '评分维度',
    phase VARCHAR(20) NOT NULL COMMENT '动作阶段',
    min_angle DECIMAL(6,2) NULL COMMENT '最小角度',
    max_angle DECIMAL(6,2) NULL COMMENT '最大角度',
    standard_angle DECIMAL(6,2) NULL COMMENT '标准角度',
    tolerance_soft DECIMAL(6,2) NULL COMMENT '软容差',
    tolerance_hard DECIMAL(6,2) NULL COMMENT '硬容差',
    hold_frames_required INT NULL COMMENT '保持所需帧数',
    speed_min DECIMAL(6,2) NULL COMMENT '最小角速度',
    speed_max DECIMAL(6,2) NULL COMMENT '最大角速度',
    score_weight DECIMAL(5,4) NOT NULL COMMENT '评分权重',
    severity TINYINT NOT NULL COMMENT '纠错严重度',
    prompt_text VARCHAR(255) NOT NULL COMMENT '纠错提示文案',
    symmetry_pair_rule_id BIGINT NULL COMMENT '对称配对规则ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (action_rule_id),
    UNIQUE KEY uk_rule_dimension (action_version_id,joint_name,dimension),
    UNIQUE KEY uk_rule_id_version (action_rule_id,action_version_id),
    CONSTRAINT fk_rule_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
    CONSTRAINT fk_rule_pair FOREIGN KEY (symmetry_pair_rule_id,action_version_id)
        REFERENCES action_rule(action_rule_id,action_version_id),
    CONSTRAINT chk_rule_pair_not_self CHECK (symmetry_pair_rule_id IS NULL OR symmetry_pair_rule_id <> action_rule_id),
    CONSTRAINT chk_rule_weight CHECK (score_weight >= 0 AND score_weight <= 1),
    CONSTRAINT chk_rule_severity CHECK (severity >= 0)
) COMMENT='动作评估规则';

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

CREATE TABLE action_template (
    template_id BIGINT NOT NULL COMMENT '主键ID',
    action_version_id BIGINT NOT NULL COMMENT '动作版本ID',
    source_type VARCHAR(20) NOT NULL COMMENT '模板来源类型',
    frame_rate DECIMAL(6,2) NOT NULL COMMENT '模板帧率',
    frame_count INT NOT NULL COMMENT '模板帧数',
    feature_dim INT NOT NULL COMMENT '特征维度',
    feature_sequence JSON NOT NULL COMMENT '归一化特征序列',
    keyframe_index JSON NULL COMMENT '关键帧索引',
    sample_count INT NOT NULL COMMENT '样本数量',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (template_id),
    UNIQUE KEY uk_template_source (action_version_id,source_type),
    CONSTRAINT fk_template_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
    CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id)
) COMMENT='标准动作模板';

CREATE TABLE action_template_sample (
    template_sample_id BIGINT NOT NULL COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    file_id BIGINT NOT NULL COMMENT '文件对象ID',
    sample_no INT NOT NULL COMMENT '采样序号',
    feature_hash CHAR(64) NOT NULL COMMENT '特征哈希',
    captured_at DATETIME NOT NULL COMMENT '采集时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    PRIMARY KEY (template_sample_id),
    UNIQUE KEY uk_template_sample (template_id,sample_no),
    CONSTRAINT fk_sample_template FOREIGN KEY (template_id) REFERENCES action_template(template_id),
    CONSTRAINT fk_sample_file FOREIGN KEY (file_id) REFERENCES file_object(file_id),
    CONSTRAINT fk_sample_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id)
) COMMENT='动作模板采样记录';

-- ============================================
-- 模块三：计划与训练闭环
-- ============================================

CREATE TABLE rehab_plan (
    plan_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID',
    therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    rehab_part_code VARCHAR(30) NOT NULL COMMENT '康复部位编码',
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
    CONSTRAINT fk_plan_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_plan_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_plan_part FOREIGN KEY (rehab_part_code) REFERENCES rehab_part_dict(code),
    CONSTRAINT chk_plan_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_plan_version CHECK (version_no > 0)
) COMMENT='康复计划';

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
    CONSTRAINT fk_record_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
    CONSTRAINT chk_record_time CHECK (ended_at >= started_at),
    CONSTRAINT chk_record_reps CHECK (total_reps >= 0 AND correct_reps BETWEEN 0 AND total_reps),
    CONSTRAINT chk_record_score CHECK (total_score BETWEEN 0 AND 100)
) COMMENT='训练记录';

CREATE TABLE training_error_log (
    error_id BIGINT NOT NULL COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '训练记录ID',
    rule_id BIGINT NULL COMMENT '动作规则ID',
    dimension VARCHAR(20) NOT NULL COMMENT '评分维度',
    phase VARCHAR(20) NOT NULL COMMENT '动作阶段',
    error_type VARCHAR(40) NOT NULL COMMENT '错误类型',
    joint_name VARCHAR(50) NULL COMMENT '关节名称',
    current_value DECIMAL(8,3) NULL COMMENT '当前值',
    expected_value DECIMAL(8,3) NULL COMMENT '期望值',
    deviation DECIMAL(8,3) NULL COMMENT '偏差值',
    severity TINYINT NOT NULL COMMENT '纠错严重度',
    frame_no INT NULL COMMENT '触发帧号',
    prompt_text VARCHAR(255) NOT NULL COMMENT '纠错提示文案',
    error_at DATETIME NOT NULL COMMENT '错误发生时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (error_id),
    KEY idx_error_record (record_id),
    KEY idx_error_dimension (dimension,severity),
    CONSTRAINT fk_error_record FOREIGN KEY (record_id) REFERENCES training_record(record_id),
    CONSTRAINT fk_error_rule FOREIGN KEY (rule_id) REFERENCES action_rule(action_rule_id)
) COMMENT='训练纠错日志';

-- ============================================
-- 模块四：报告与内容
-- ============================================

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

CREATE TABLE content (
    content_id BIGINT NOT NULL COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content_type VARCHAR(30) NOT NULL COMMENT '内容类型',
    target_part_code VARCHAR(30) NOT NULL COMMENT '目标部位编码',
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
    CONSTRAINT fk_content_part FOREIGN KEY (target_part_code) REFERENCES rehab_part_dict(code),
    CONSTRAINT fk_content_author FOREIGN KEY (author_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_content_reviewer FOREIGN KEY (reviewer_id) REFERENCES user_account(user_id),
    CONSTRAINT chk_content_valid_period CHECK (valid_to IS NULL OR valid_to >= valid_from)
) COMMENT='公共健康内容';

CREATE TABLE content_attachment (
    content_attachment_id BIGINT NOT NULL COMMENT '主键ID',
    content_id BIGINT NOT NULL COMMENT '内容ID',
    file_id BIGINT NOT NULL COMMENT '文件对象ID',
    attachment_type VARCHAR(20) NOT NULL COMMENT '附件类型',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (content_attachment_id),
    UNIQUE KEY uk_content_file (content_id,file_id),
    CONSTRAINT fk_ca_content FOREIGN KEY (content_id) REFERENCES content(content_id),
    CONSTRAINT fk_ca_file FOREIGN KEY (file_id) REFERENCES file_object(file_id)
) COMMENT='内容附件';

CREATE TABLE content_collection (
    collection_id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户账号ID',
    content_id BIGINT NOT NULL COMMENT '内容ID',
    collected_at DATETIME NOT NULL COMMENT '收藏时间',
    PRIMARY KEY (collection_id),
    UNIQUE KEY uk_collection_user_content (user_id,content_id),
    CONSTRAINT fk_collection_user FOREIGN KEY (user_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_collection_content FOREIGN KEY (content_id) REFERENCES content(content_id)
) COMMENT='内容收藏';

-- ============================================
-- 模块五：咨询与文件
-- ============================================

CREATE TABLE consultation (
    consultation_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID',
    therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    closed_at DATETIME NULL COMMENT '关闭时间',
    PRIMARY KEY (consultation_id),
    KEY idx_consult_patient (patient_id,status),
    KEY idx_consult_therapist (therapist_id,status),
    CONSTRAINT fk_consult_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_consult_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id)
) COMMENT='咨询会话';

CREATE TABLE consultation_message (
    message_id BIGINT NOT NULL COMMENT '主键ID',
    consultation_id BIGINT NOT NULL COMMENT '咨询会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    content TEXT NULL COMMENT '消息内容',
    is_ai_draft TINYINT NOT NULL DEFAULT 0 COMMENT 'AI草稿标记',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '已读标记',
    read_at DATETIME NULL COMMENT '已读时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (message_id),
    KEY idx_message_consult_time (consultation_id,created_at),
    KEY idx_message_unread (sender_id,is_read),
    CONSTRAINT fk_message_consult FOREIGN KEY (consultation_id) REFERENCES consultation(consultation_id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES user_account(user_id)
) COMMENT='咨询消息';

CREATE TABLE consultation_attachment (
    consultation_attachment_id BIGINT NOT NULL COMMENT '主键ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    file_id BIGINT NOT NULL COMMENT '文件对象ID',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (consultation_attachment_id),
    UNIQUE KEY uk_message_file (message_id,file_id),
    CONSTRAINT fk_ma_message FOREIGN KEY (message_id) REFERENCES consultation_message(message_id),
    CONSTRAINT fk_ma_file FOREIGN KEY (file_id) REFERENCES file_object(file_id)
) COMMENT='咨询消息附件';

-- ============================================
-- 模块六：审计与公告
-- ============================================

CREATE TABLE operation_log (
    log_id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NULL COMMENT '用户账号ID',
    module VARCHAR(30) NOT NULL COMMENT '业务模块',
    action VARCHAR(50) NOT NULL COMMENT '操作动作',
    target_type VARCHAR(50) NOT NULL COMMENT '目标类型',
    target_id BIGINT NULL COMMENT '目标对象ID',
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

CREATE TABLE announcement (
    announcement_id BIGINT NOT NULL COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content LONGTEXT NOT NULL COMMENT '公告正文',
    scope VARCHAR(20) NOT NULL COMMENT '展示范围',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    start_time DATETIME NOT NULL COMMENT '开始展示时间',
    end_time DATETIME NOT NULL COMMENT '结束展示时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (announcement_id),
    KEY idx_notice_scope_status (scope,status,start_time,end_time),
    CONSTRAINT fk_notice_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id),
    CONSTRAINT chk_notice_period CHECK (end_time >= start_time)
) COMMENT='平台公告';

-- 绑定关系完整性：计划和咨询必须引用同一条患者-治疗师绑定记录
ALTER TABLE patient_therapist
    ADD UNIQUE KEY uk_binding_id_pair (binding_id, patient_id, therapist_id);

ALTER TABLE rehab_plan
    ADD COLUMN binding_id BIGINT NOT NULL COMMENT '患者治疗师绑定ID' AFTER therapist_id,
    ADD KEY idx_plan_binding (binding_id),
    ADD CONSTRAINT fk_plan_binding_pair
        FOREIGN KEY (binding_id, patient_id, therapist_id)
        REFERENCES patient_therapist (binding_id, patient_id, therapist_id);

ALTER TABLE consultation
    ADD COLUMN binding_id BIGINT NOT NULL COMMENT '患者治疗师绑定ID' AFTER therapist_id,
    ADD KEY idx_consult_binding (binding_id),
    ADD CONSTRAINT fk_consult_binding_pair
        FOREIGN KEY (binding_id, patient_id, therapist_id)
        REFERENCES patient_therapist (binding_id, patient_id, therapist_id);

-- 应用未提供 ID 时由数据库自动生成（rehab_part_dict.code 为业务编码，保留手工赋值）
ALTER TABLE user_account MODIFY user_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE patient_profile MODIFY patient_profile_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE patient_rehab_part MODIFY patient_rehab_part_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE therapist_profile MODIFY therapist_profile_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE patient_therapist MODIFY binding_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action MODIFY action_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_version MODIFY action_version_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_rule MODIFY action_rule_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE file_object MODIFY file_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_template MODIFY template_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_template_sample MODIFY template_sample_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE rehab_plan MODIFY plan_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE plan_item MODIFY plan_item_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE plan_schedule MODIFY schedule_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE plan_daily_task MODIFY task_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE training_record MODIFY record_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE training_error_log MODIFY error_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE assessment_report MODIFY report_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE content MODIFY content_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE content_attachment MODIFY content_attachment_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE content_collection MODIFY collection_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE consultation MODIFY consultation_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE consultation_message MODIFY message_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE consultation_attachment MODIFY consultation_attachment_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE operation_log MODIFY log_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE announcement MODIFY announcement_id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS=1;
