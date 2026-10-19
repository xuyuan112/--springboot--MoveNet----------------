-- PowerDesigner逆向工程 MySQL DDL（仅表结构，无数据）
CREATE DATABASE IF NOT EXISTS rehab_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rehab_platform;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE user_account (
id BIGINT NOT NULL COMMENT '用户账号ID',
  username VARCHAR(50) NOT NULL COMMENT '登录名',
  password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt密码密文',
  role_code VARCHAR(20) NOT NULL COMMENT '角色：PATIENT/THERAPIST/ADMIN',
  phone VARCHAR(20) NULL COMMENT '手机号',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用0禁用',
  last_login_at DATETIME NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL COMMENT '创建时间', 
  updated_at DATETIME NOT NULL COMMENT '更新时间', 
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

CREATE TABLE rehab_part_dict (
code VARCHAR(30) NOT NULL COMMENT '康复部位编码',
  name VARCHAR(50) NOT NULL COMMENT '康复部位名称',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号', 
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用标记', 
  created_at DATETIME NOT NULL COMMENT '创建时间', 
  updated_at DATETIME NOT NULL COMMENT '更新时间'
);

CREATE TABLE patient_profile (
id BIGINT NOT NULL COMMENT '主键ID', 
  user_id BIGINT NOT NULL COMMENT '用户账号ID', 
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名', 
  gender CHAR(1) NOT NULL COMMENT '性别', 
  birth_date DATE NOT NULL COMMENT '出生日期', 
  height_cm DECIMAL(5,2) NULL COMMENT '身高厘米',
  weight_kg DECIMAL(5,2) NULL COMMENT '体重千克',
  rehab_part_code VARCHAR(30) NOT NULL COMMENT '康复部位编码', 
  medical_history TEXT NULL COMMENT '病史摘要', 
  allergy_info TEXT NULL COMMENT '过敏信息', 
  contraindication TEXT NULL COMMENT '禁忌说明', 
  created_at DATETIME NOT NULL COMMENT '创建时间', 
  updated_at DATETIME NOT NULL COMMENT '更新时间', 
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

CREATE TABLE therapist_profile (
id BIGINT NOT NULL COMMENT '主键ID', 
  user_id BIGINT NOT NULL COMMENT '用户账号ID', 
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名', 
  title VARCHAR(50) NOT NULL COMMENT '职称', 
  specialty VARCHAR(100) NOT NULL COMMENT '专业特长', 
  license_no VARCHAR(100) NULL COMMENT '执业资质编号', 
  introduction TEXT NULL COMMENT '个人简介', 
  created_at DATETIME NOT NULL COMMENT '创建时间', 
  updated_at DATETIME NOT NULL COMMENT '更新时间', 
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

CREATE TABLE patient_therapist (
id BIGINT NOT NULL COMMENT '主键ID', 
  patient_id BIGINT NOT NULL COMMENT '患者用户ID', 
  therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID', 
  relation_type VARCHAR(20) NOT NULL COMMENT '关系类型', 
  status VARCHAR(20) NOT NULL COMMENT '状态', 
  start_at DATETIME NOT NULL COMMENT '生效时间', 
  end_at DATETIME NULL COMMENT '结束时间', 
  created_by BIGINT NOT NULL COMMENT '创建人ID', 
  created_at DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE action (
id BIGINT NOT NULL COMMENT '主键ID', 
  name VARCHAR(100) NOT NULL COMMENT '名称', 
  target_part_code VARCHAR(30) NOT NULL COMMENT '目标部位编码', 
  difficulty VARCHAR(20) NOT NULL COMMENT '动作难度', 
  description TEXT NULL COMMENT '动作描述', 
  status VARCHAR(20) NOT NULL COMMENT '状态', 
  created_by BIGINT NOT NULL COMMENT '创建人ID', 
  created_at DATETIME NOT NULL COMMENT '创建时间', 
  updated_at DATETIME NOT NULL COMMENT '更新时间', 
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

CREATE TABLE action_version (
id BIGINT NOT NULL COMMENT '主键ID', 
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
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

CREATE TABLE action_rule (
id BIGINT NOT NULL COMMENT '主键ID', 
  action_version_id BIGINT NOT NULL COMMENT '动作版本ID', 
  joint_name VARCHAR(50) NOT NULL COMMENT '关节名称', 
  keypoint_indices JSON NOT NULL,
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
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

CREATE TABLE file_object (
id BIGINT NOT NULL COMMENT '主键ID', 
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
  deleted_at DATETIME NULL COMMENT '删除时间'
);

CREATE TABLE action_template (
id BIGINT NOT NULL COMMENT '主键ID', 
  action_version_id BIGINT NOT NULL COMMENT '动作版本ID', 
  source_type VARCHAR(20) NOT NULL COMMENT '模板来源类型', 
  frame_rate DECIMAL(6,2) NOT NULL COMMENT '模板帧率',
  frame_count INT NOT NULL COMMENT '模板帧数', 
  feature_dim INT NOT NULL COMMENT '特征维度', 
  feature_sequence JSON NOT NULL COMMENT '归一化特征序列', 
  keyframe_index JSON NULL,
  sample_count INT NOT NULL COMMENT '样本数量', 
  status VARCHAR(20) NOT NULL COMMENT '状态', 
  created_by BIGINT NOT NULL COMMENT '创建人ID', 
  created_at DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE action_template_sample (
id BIGINT NOT NULL COMMENT '主键ID', 
  template_id BIGINT NOT NULL COMMENT '模板ID', 
  file_id BIGINT NOT NULL COMMENT '文件对象ID', 
  sample_no INT NOT NULL COMMENT '采样序号', 
  feature_hash CHAR(64) NOT NULL COMMENT '特征哈希', 
  captured_at DATETIME NOT NULL COMMENT '采集时间', 
  created_by BIGINT NOT NULL COMMENT '创建人ID'
);

CREATE TABLE rehab_plan (
id BIGINT NOT NULL COMMENT '主键ID', 
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
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

CREATE TABLE plan_item (
id BIGINT NOT NULL COMMENT '主键ID', 
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
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

CREATE TABLE plan_schedule (
id BIGINT NOT NULL COMMENT '主键ID', 
  plan_id BIGINT NOT NULL COMMENT '康复计划ID', 
  weekday TINYINT NOT NULL COMMENT '执行星期1至7',
  created_at DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE plan_daily_task (
id BIGINT NOT NULL COMMENT '主键ID', 
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
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

CREATE TABLE training_record (
id BIGINT NOT NULL COMMENT '主键ID', 
  daily_task_id BIGINT NOT NULL COMMENT '每日任务ID', 
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
  created_at DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE training_error_log (
id BIGINT NOT NULL COMMENT '主键ID', 
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
  created_at DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE assessment_report (
id BIGINT NOT NULL COMMENT '主键ID', 
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
  updated_at DATETIME NOT NULL COMMENT '更新时间'
);

CREATE TABLE content (
id BIGINT NOT NULL COMMENT '主键ID', 
  title VARCHAR(200) NOT NULL COMMENT '职称', 
  content_type VARCHAR(30) NOT NULL COMMENT '内容/文件类型', 
  target_part_code VARCHAR(30) NOT NULL COMMENT '目标部位编码', 
  target_group VARCHAR(100) NULL COMMENT '适用人群', 
  summary VARCHAR(500) NULL COMMENT '内容摘要', 
  content_body LONGTEXT NOT NULL COMMENT '内容正文', 
  author_id BIGINT NOT NULL COMMENT '内容作者ID',
  status VARCHAR(20) NOT NULL COMMENT '状态', 
  reviewer_id BIGINT NULL COMMENT '审核人ID', 
  review_remark VARCHAR(500) NULL COMMENT '审核意见', 
  valid_from DATETIME NOT NULL COMMENT '有效开始时间', 
  valid_to DATETIME NOT NULL COMMENT '有效结束时间', 
  published_at DATETIME NULL COMMENT '发布时间', 
  created_at DATETIME NOT NULL COMMENT '创建时间', 
  updated_at DATETIME NOT NULL COMMENT '更新时间', 
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

CREATE TABLE content_attachment (
id BIGINT NOT NULL COMMENT '主键ID', 
  content_id BIGINT NOT NULL COMMENT '内容ID', 
  file_id BIGINT NOT NULL COMMENT '文件对象ID', 
  attachment_type VARCHAR(20) NOT NULL COMMENT '附件类型', 
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号', 
  created_at DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE content_collection (
id BIGINT NOT NULL COMMENT '主键ID', 
  user_id BIGINT NOT NULL COMMENT '用户账号ID', 
  content_id BIGINT NOT NULL COMMENT '内容ID', 
  collected_at DATETIME NOT NULL COMMENT '收藏时间'
);

CREATE TABLE consultation (
id BIGINT NOT NULL COMMENT '主键ID', 
  patient_id BIGINT NOT NULL COMMENT '患者用户ID', 
  therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID', 
  status VARCHAR(20) NOT NULL COMMENT '状态', 
  created_at DATETIME NOT NULL COMMENT '创建时间', 
  closed_at DATETIME NULL COMMENT '关闭时间'
);

CREATE TABLE consultation_message (
id BIGINT NOT NULL COMMENT '主键ID', 
  consultation_id BIGINT NOT NULL COMMENT '咨询会话ID', 
  sender_id BIGINT NOT NULL COMMENT '发送者ID', 
  content TEXT NULL COMMENT '消息内容',
  is_ai_draft TINYINT NOT NULL DEFAULT 0 COMMENT 'AI草稿标记', 
  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '已读标记', 
  read_at DATETIME NULL COMMENT '已读时间', 
  created_at DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE consultation_attachment (
id BIGINT NOT NULL COMMENT '主键ID', 
  message_id BIGINT NOT NULL COMMENT '消息ID', 
  file_id BIGINT NOT NULL COMMENT '文件对象ID', 
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号', 
  created_at DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE operation_log (
id BIGINT NOT NULL COMMENT '主键ID', 
  user_id BIGINT NULL COMMENT '用户账号ID', 
  module VARCHAR(30) NOT NULL COMMENT '业务模块', 
  action VARCHAR(50) NOT NULL COMMENT '操作动作',
  target_type VARCHAR(50) NOT NULL COMMENT '目标类型', 
  target_id BIGINT NULL COMMENT '目标对象ID', 
  ip VARCHAR(45) NOT NULL COMMENT '客户端IP', 
  device_id VARCHAR(100) NULL COMMENT '设备标识', 
  status VARCHAR(20) NOT NULL COMMENT '状态', 
  detail JSON NULL COMMENT '操作详情', 
  created_at DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE announcement (
id BIGINT NOT NULL COMMENT '主键ID', 
  title VARCHAR(200) NOT NULL COMMENT '职称', 
  content LONGTEXT NOT NULL COMMENT '公告正文',
  scope VARCHAR(20) NOT NULL COMMENT '展示范围',
  status VARCHAR(20) NOT NULL COMMENT '状态', 
  start_time DATETIME NOT NULL COMMENT '开始展示时间',
  end_time DATETIME NOT NULL COMMENT '结束展示时间',
  created_by BIGINT NOT NULL COMMENT '创建人ID', 
  created_at DATETIME NOT NULL COMMENT '创建时间', 
  updated_at DATETIME NOT NULL COMMENT '更新时间', 
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
);

ALTER TABLE user_account ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_user_username (username), ADD UNIQUE KEY uk_user_phone (phone);
ALTER TABLE rehab_part_dict ADD PRIMARY KEY (code), ADD UNIQUE KEY uk_part_name (name);
ALTER TABLE patient_profile ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_patient_user (user_id), ADD KEY idx_patient_part (rehab_part_code);
ALTER TABLE therapist_profile ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_therapist_user (user_id), ADD UNIQUE KEY uk_therapist_license (license_no);
ALTER TABLE patient_therapist ADD PRIMARY KEY (id), ADD KEY idx_binding_patient (patient_id,status), ADD KEY idx_binding_therapist (therapist_id,status);
ALTER TABLE action ADD PRIMARY KEY (id), ADD KEY idx_action_part_status (target_part_code,status);
ALTER TABLE action_version ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_action_version (action_id,version_no), ADD KEY idx_action_version_status (action_id,status);
ALTER TABLE action_rule ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_rule_dimension (action_version_id,joint_name,dimension);
ALTER TABLE file_object ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_file_object_key (object_key), ADD KEY idx_file_purpose (purpose,scan_status);
ALTER TABLE action_template ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_template_source (action_version_id,source_type);
ALTER TABLE action_template_sample ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_template_sample (template_id,sample_no);
ALTER TABLE rehab_plan ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_plan_version (patient_id,name,version_no), ADD KEY idx_plan_patient_status (patient_id,status);
ALTER TABLE plan_item ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_plan_item_sequence (plan_id,sequence_no);
ALTER TABLE plan_schedule ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_schedule_day (plan_id,weekday);
ALTER TABLE plan_daily_task ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_task_item_date (plan_item_id,task_date), ADD KEY idx_task_date_status (task_date,status);
ALTER TABLE training_record ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_record_idempotency (daily_task_id,client_request_id), ADD KEY idx_record_task_time (daily_task_id,created_at);
ALTER TABLE training_error_log ADD PRIMARY KEY (id), ADD KEY idx_error_record (record_id), ADD KEY idx_error_dimension (dimension,severity);
ALTER TABLE assessment_report ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_report_period (plan_id,period_start,period_end);
ALTER TABLE content ADD PRIMARY KEY (id), ADD KEY idx_content_filter (status,target_part_code,valid_from,valid_to);
ALTER TABLE content_attachment ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_content_file (content_id,file_id);
ALTER TABLE content_collection ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_collection_user_content (user_id,content_id);
ALTER TABLE consultation ADD PRIMARY KEY (id), ADD KEY idx_consult_patient (patient_id,status), ADD KEY idx_consult_therapist (therapist_id,status);
ALTER TABLE consultation_message ADD PRIMARY KEY (id), ADD KEY idx_message_consult_time (consultation_id,created_at), ADD KEY idx_message_unread (sender_id,is_read);
ALTER TABLE consultation_attachment ADD PRIMARY KEY (id), ADD UNIQUE KEY uk_message_file (message_id,file_id);
ALTER TABLE operation_log ADD PRIMARY KEY (id), ADD KEY idx_log_user_time (user_id,created_at), ADD KEY idx_log_module_time (module,created_at);
ALTER TABLE announcement ADD PRIMARY KEY (id), ADD KEY idx_notice_scope_status (scope,status,start_time,end_time);ALTER TABLE patient_profile ADD CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES user_account(id), ADD CONSTRAINT fk_patient_part FOREIGN KEY (rehab_part_code) REFERENCES rehab_part_dict(code);
ALTER TABLE therapist_profile ADD CONSTRAINT fk_therapist_user FOREIGN KEY (user_id) REFERENCES user_account(id);
ALTER TABLE patient_therapist ADD CONSTRAINT fk_binding_patient FOREIGN KEY (patient_id) REFERENCES user_account(id), ADD CONSTRAINT fk_binding_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(id), ADD CONSTRAINT fk_binding_creator FOREIGN KEY (created_by) REFERENCES user_account(id);
ALTER TABLE action ADD CONSTRAINT fk_action_part FOREIGN KEY (target_part_code) REFERENCES rehab_part_dict(code), ADD CONSTRAINT fk_action_creator FOREIGN KEY (created_by) REFERENCES user_account(id);
ALTER TABLE action_version ADD CONSTRAINT fk_av_action FOREIGN KEY (action_id) REFERENCES action(id), ADD CONSTRAINT fk_av_creator FOREIGN KEY (created_by) REFERENCES user_account(id);
ALTER TABLE action_rule ADD CONSTRAINT fk_rule_version FOREIGN KEY (action_version_id) REFERENCES action_version(id), ADD CONSTRAINT fk_rule_pair FOREIGN KEY (symmetry_pair_rule_id) REFERENCES action_rule(id);
ALTER TABLE file_object ADD CONSTRAINT fk_file_creator FOREIGN KEY (created_by) REFERENCES user_account(id);
ALTER TABLE action_template ADD CONSTRAINT fk_template_version FOREIGN KEY (action_version_id) REFERENCES action_version(id), ADD CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES user_account(id);
ALTER TABLE action_template_sample ADD CONSTRAINT fk_sample_template FOREIGN KEY (template_id) REFERENCES action_template(id), ADD CONSTRAINT fk_sample_file FOREIGN KEY (file_id) REFERENCES file_object(id), ADD CONSTRAINT fk_sample_creator FOREIGN KEY (created_by) REFERENCES user_account(id);
ALTER TABLE rehab_plan ADD CONSTRAINT fk_plan_patient FOREIGN KEY (patient_id) REFERENCES user_account(id), ADD CONSTRAINT fk_plan_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(id), ADD CONSTRAINT fk_plan_part FOREIGN KEY (rehab_part_code) REFERENCES rehab_part_dict(code);
ALTER TABLE plan_item ADD CONSTRAINT fk_item_plan FOREIGN KEY (plan_id) REFERENCES rehab_plan(id), ADD CONSTRAINT fk_item_version FOREIGN KEY (action_version_id) REFERENCES action_version(id);
ALTER TABLE plan_schedule ADD CONSTRAINT fk_schedule_plan FOREIGN KEY (plan_id) REFERENCES rehab_plan(id);
ALTER TABLE plan_daily_task ADD CONSTRAINT fk_task_item FOREIGN KEY (plan_item_id) REFERENCES plan_item(id);
ALTER TABLE training_record ADD CONSTRAINT fk_record_task FOREIGN KEY (daily_task_id) REFERENCES plan_daily_task(id);
ALTER TABLE training_error_log ADD CONSTRAINT fk_error_record FOREIGN KEY (record_id) REFERENCES training_record(id), ADD CONSTRAINT fk_error_rule FOREIGN KEY (rule_id) REFERENCES action_rule(id);
ALTER TABLE assessment_report ADD CONSTRAINT fk_report_plan FOREIGN KEY (plan_id) REFERENCES rehab_plan(id), ADD CONSTRAINT fk_report_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account(id);
ALTER TABLE content ADD CONSTRAINT fk_content_part FOREIGN KEY (target_part_code) REFERENCES rehab_part_dict(code), ADD CONSTRAINT fk_content_author FOREIGN KEY (author_id) REFERENCES user_account(id), ADD CONSTRAINT fk_content_reviewer FOREIGN KEY (reviewer_id) REFERENCES user_account(id);
ALTER TABLE content_attachment ADD CONSTRAINT fk_ca_content FOREIGN KEY (content_id) REFERENCES content(id), ADD CONSTRAINT fk_ca_file FOREIGN KEY (file_id) REFERENCES file_object(id);
ALTER TABLE content_collection ADD CONSTRAINT fk_collection_user FOREIGN KEY (user_id) REFERENCES user_account(id), ADD CONSTRAINT fk_collection_content FOREIGN KEY (content_id) REFERENCES content(id);
ALTER TABLE consultation ADD CONSTRAINT fk_consult_patient FOREIGN KEY (patient_id) REFERENCES user_account(id), ADD CONSTRAINT fk_consult_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(id);
ALTER TABLE consultation_message ADD CONSTRAINT fk_message_consult FOREIGN KEY (consultation_id) REFERENCES consultation(id), ADD CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES user_account(id);
ALTER TABLE consultation_attachment ADD CONSTRAINT fk_ma_message FOREIGN KEY (message_id) REFERENCES consultation_message(id), ADD CONSTRAINT fk_ma_file FOREIGN KEY (file_id) REFERENCES file_object(id);
ALTER TABLE operation_log ADD CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES user_account(id);
ALTER TABLE announcement ADD CONSTRAINT fk_notice_creator FOREIGN KEY (created_by) REFERENCES user_account(id);
SET FOREIGN_KEY_CHECKS=1;

-- 表中文注释（PowerDesigner 可读取）
ALTER TABLE user_account COMMENT='用户账号';
ALTER TABLE rehab_part_dict COMMENT='康复部位字典';
ALTER TABLE patient_profile COMMENT='患者档案';
ALTER TABLE therapist_profile COMMENT='治疗师档案';
ALTER TABLE patient_therapist COMMENT='患者治疗师关系';
ALTER TABLE action COMMENT='标准动作';
ALTER TABLE action_version COMMENT='动作版本';
ALTER TABLE action_rule COMMENT='动作评估规则';
ALTER TABLE file_object COMMENT='MinIO文件对象';
ALTER TABLE action_template COMMENT='标准动作模板';
ALTER TABLE action_template_sample COMMENT='动作模板采样记录';
ALTER TABLE rehab_plan COMMENT='康复计划';
ALTER TABLE plan_item COMMENT='计划动作项';
ALTER TABLE plan_schedule COMMENT='计划执行日程';
ALTER TABLE plan_daily_task COMMENT='每日训练任务';
ALTER TABLE training_record COMMENT='训练记录';
ALTER TABLE training_error_log COMMENT='训练纠错日志';
ALTER TABLE assessment_report COMMENT='评估报告';
ALTER TABLE content COMMENT='公共健康内容';
ALTER TABLE content_attachment COMMENT='内容附件';
ALTER TABLE content_collection COMMENT='内容收藏';
ALTER TABLE consultation COMMENT='咨询会话';
ALTER TABLE consultation_message COMMENT='咨询消息';
ALTER TABLE consultation_attachment COMMENT='咨询消息附件';
ALTER TABLE operation_log COMMENT='操作审计日志';
ALTER TABLE announcement COMMENT='平台公告';
