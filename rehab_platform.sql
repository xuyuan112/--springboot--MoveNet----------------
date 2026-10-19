-- =====================================================================
-- 康复训练平台数据库结构（MySQL 8.0+ 最终定稿版）
-- 说明：
--   1. 本脚本面向全新空库一次性执行，建表顺序已按外键依赖排列，无需关闭外键检查；
--   2. 所有主键在建表时直接声明 AUTO_INCREMENT，应用层可不传 ID；
--   3. created_at / updated_at 已设置数据库级默认值与自动更新，应用层可省略赋值；
--   4. 如需重复执行，请先取消下一行注释（会清空整个库，谨慎！）。
-- DROP DATABASE IF EXISTS rehab_platform;
-- =====================================================================
CREATE DATABASE IF NOT EXISTS rehab_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rehab_platform;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 用户与档案
-- ---------------------------------------------------------------------
CREATE TABLE user_account (
  user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户账号ID',
  username VARCHAR(50) NOT NULL COMMENT '登录名',
  password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt密码密文',
  role_code VARCHAR(20) NOT NULL COMMENT '角色：PATIENT/THERAPIST/ADMIN',
  phone VARCHAR(20) NULL COMMENT '手机号',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用0禁用',
  last_login_at DATETIME(3) NULL COMMENT '最后登录时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_phone (phone),
  CONSTRAINT chk_user_role CHECK (role_code IN ('PATIENT','THERAPIST','ADMIN')),
  CONSTRAINT chk_user_status CHECK (status IN (0,1)),
  CONSTRAINT chk_user_deleted CHECK (deleted IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号';

CREATE TABLE patient_profile (
  patient_profile_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '患者档案ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户账号ID',
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
  gender CHAR(1) NOT NULL COMMENT '性别：M男/F女/U未知',
  birth_date DATE NOT NULL COMMENT '出生日期',
  height_cm DECIMAL(5,2) NULL COMMENT '身高(cm)',
  weight_kg DECIMAL(5,2) NULL COMMENT '体重(kg)',
  medical_history TEXT NULL COMMENT '既往病史',
  allergy_info TEXT NULL COMMENT '过敏史',
  contraindication TEXT NULL COMMENT '禁忌症',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (patient_profile_id),
  UNIQUE KEY uk_patient_user (user_id),
  CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES user_account(user_id),
  CONSTRAINT chk_patient_gender CHECK (gender IN ('M','F','U')),
  CONSTRAINT chk_patient_height CHECK (height_cm IS NULL OR height_cm > 0),
  CONSTRAINT chk_patient_weight CHECK (weight_kg IS NULL OR weight_kg > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者档案';

CREATE TABLE therapist_profile (
  therapist_profile_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '治疗师档案ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户账号ID',
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
  title VARCHAR(50) NOT NULL COMMENT '职称',
  specialty VARCHAR(100) NOT NULL COMMENT '专长领域',
  license_no VARCHAR(100) NULL COMMENT '执业证书编号',
  introduction TEXT NULL COMMENT '个人简介',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (therapist_profile_id),
  UNIQUE KEY uk_therapist_user (user_id),
  UNIQUE KEY uk_therapist_license (license_no),
  CONSTRAINT fk_therapist_user FOREIGN KEY (user_id) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗师档案';

-- 患者-治疗师绑定关系
-- uk_binding_id_pair 供 rehab_plan / consultation 的组合外键引用，
-- 保证计划与咨询必须挂在同一条有效的患者-治疗师绑定记录上。
CREATE TABLE patient_therapist (
  binding_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '绑定关系ID',
  patient_id BIGINT UNSIGNED NOT NULL COMMENT '患者用户ID',
  therapist_id BIGINT UNSIGNED NOT NULL COMMENT '治疗师用户ID',
  relation_type VARCHAR(20) NOT NULL COMMENT '关系类型：PRIMARY主治/SECONDARY协作',
  status VARCHAR(20) NOT NULL COMMENT '状态：ACTIVE/ENDED',
  start_at DATETIME(3) NOT NULL COMMENT '绑定开始时间',
  end_at DATETIME(3) NULL COMMENT '绑定结束时间',
  created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (binding_id),
  UNIQUE KEY uk_binding_id_pair (binding_id, patient_id, therapist_id),
  KEY idx_binding_patient (patient_id, status),
  KEY idx_binding_therapist (therapist_id, status),
  CONSTRAINT fk_binding_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
  CONSTRAINT fk_binding_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id),
  CONSTRAINT fk_binding_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id),
  CONSTRAINT chk_binding_period CHECK (end_at IS NULL OR end_at >= start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者治疗师关系';

-- ---------------------------------------------------------------------
-- 2. 康复部位字典与患者登记
-- ---------------------------------------------------------------------
CREATE TABLE rehab_part_dict (
  code VARCHAR(30) NOT NULL COMMENT '康复部位编码',
  name VARCHAR(50) NOT NULL COMMENT '康复部位名称',
  group_code VARCHAR(20) NOT NULL COMMENT '部位大类：UPPER上肢/LOWER下肢/SPINE脊柱/OVERALL全身综合',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1是0否',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (code),
  UNIQUE KEY uk_part_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='康复部位字典';

-- 患者康复部位（多对多）：一个患者可同时登记多个部位，侧别由 side 表达
CREATE TABLE patient_rehab_part (
  patient_rehab_part_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '登记ID',
  patient_id BIGINT UNSIGNED NOT NULL COMMENT '患者用户ID(user_account.user_id)',
  rehab_part_code VARCHAR(30) NOT NULL COMMENT '康复部位编码',
  side VARCHAR(10) NOT NULL DEFAULT 'BILATERAL' COMMENT '侧别 LEFT/RIGHT/BILATERAL',
  primary_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否主康复部位 1是0否',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RESOLVED',
  diagnosed_at DATE NULL COMMENT '确诊或受损日期',
  remark VARCHAR(255) NULL COMMENT '备注',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (patient_rehab_part_id),
  KEY idx_pp_patient_status (patient_id, status),
  KEY idx_pp_part_side (patient_id, rehab_part_code, side),
  KEY idx_pp_part (rehab_part_code),
  CONSTRAINT fk_pp_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
  CONSTRAINT fk_pp_part FOREIGN KEY (rehab_part_code) REFERENCES rehab_part_dict(code),
  CONSTRAINT chk_pp_side CHECK (side IN ('LEFT','RIGHT','BILATERAL')),
  CONSTRAINT chk_pp_primary CHECK (primary_flag IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者康复部位（多对多）';

-- ---------------------------------------------------------------------
-- 3. 动作库（动作 / 版本 / 评估规则 / 模板）
-- ---------------------------------------------------------------------
CREATE TABLE action (
  action_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '动作ID',
  name VARCHAR(100) NOT NULL COMMENT '动作名称',
  target_part_code VARCHAR(30) NOT NULL COMMENT '目标康复部位编码',
  difficulty VARCHAR(20) NOT NULL COMMENT '难度：EASY/MEDIUM/HARD',
  description TEXT NULL COMMENT '动作说明',
  status VARCHAR(20) NOT NULL COMMENT '状态：DRAFT/PUBLISHED/DISABLED',
  created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (action_id),
  KEY idx_action_part_status (target_part_code, status),
  CONSTRAINT fk_action_part FOREIGN KEY (target_part_code) REFERENCES rehab_part_dict(code),
  CONSTRAINT fk_action_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标准动作';

CREATE TABLE action_version (
  action_version_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '动作版本ID',
  action_id BIGINT UNSIGNED NOT NULL COMMENT '动作ID',
  version_no INT NOT NULL COMMENT '版本号',
  action_type VARCHAR(20) NOT NULL COMMENT '动作类型：ROM关节活动度/STRENGTH力量/BALANCE平衡',
  algo_config JSON NOT NULL COMMENT '算法配置（采样率、平滑窗口等）',
  motion_signature JSON NOT NULL COMMENT '运动特征签名',
  pose_engine_id VARCHAR(150) NOT NULL COMMENT '姿态引擎标识（如 movenet-lightning-v4）',
  status VARCHAR(20) NOT NULL COMMENT '状态：DRAFT/PUBLISHED/DEPRECATED',
  created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (action_version_id),
  UNIQUE KEY uk_action_version (action_id, version_no),
  KEY idx_action_version_status (action_id, status),
  CONSTRAINT fk_av_action FOREIGN KEY (action_id) REFERENCES action(action_id),
  CONSTRAINT fk_av_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动作版本';

-- 注意：原设计中的 chk_rule_pair_not_self（禁止配对规则指向自身）需要引用自增主键
-- action_rule_id，而 MySQL 禁止 CHECK 约束引用 AUTO_INCREMENT 列（错误 3818，且
-- 事后 ALTER 补自增同样会被拦截）。因此改用表后的 BEFORE INSERT/UPDATE 触发器实现
-- 同等校验，业务约束语义不变。
CREATE TABLE action_rule (
  action_rule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  action_version_id BIGINT UNSIGNED NOT NULL COMMENT '动作版本ID',
  joint_name VARCHAR(50) NOT NULL COMMENT '关节名称',
  keypoint_indices JSON NOT NULL COMMENT '参与计算的关键点索引',
  angle_formula VARCHAR(100) NOT NULL COMMENT '角度计算公式',
  joint_role VARCHAR(20) NOT NULL COMMENT '关节角色：PRIMARY主评估/AUXILIARY辅助',
  dimension VARCHAR(20) NOT NULL COMMENT '评估维度：ANGLE角度/STABILITY稳定/SYMMETRY对称/HOLD保持/SPEED速度',
  phase VARCHAR(20) NOT NULL COMMENT '动作阶段：START准备/CONCENTRIC向心/ECCENTRIC离心/HOLD保持/FULL全程',
  min_angle DECIMAL(6,2) NULL COMMENT '最小角度',
  max_angle DECIMAL(6,2) NULL COMMENT '最大角度',
  standard_angle DECIMAL(6,2) NULL COMMENT '标准角度',
  tolerance_soft DECIMAL(6,2) NULL COMMENT '软容差（提示级）',
  tolerance_hard DECIMAL(6,2) NULL COMMENT '硬容差（纠错级）',
  hold_frames_required INT NULL COMMENT '保持所需帧数',
  speed_min DECIMAL(6,2) NULL COMMENT '最小速度(度/秒)',
  speed_max DECIMAL(6,2) NULL COMMENT '最大速度(度/秒)',
  score_weight DECIMAL(5,4) NOT NULL COMMENT '评分权重(0-1)',
  severity TINYINT NOT NULL COMMENT '严重等级（数值越大越严重）',
  prompt_text VARCHAR(255) NOT NULL COMMENT '纠错提示文案',
  symmetry_pair_rule_id BIGINT UNSIGNED NULL COMMENT '对称评估配对规则ID（同版本内）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (action_rule_id),
  UNIQUE KEY uk_rule_dimension (action_version_id, joint_name, dimension),
  UNIQUE KEY uk_rule_id_version (action_rule_id, action_version_id),
  CONSTRAINT fk_rule_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
  CONSTRAINT fk_rule_pair FOREIGN KEY (symmetry_pair_rule_id, action_version_id)
    REFERENCES action_rule(action_rule_id, action_version_id),
  CONSTRAINT chk_rule_weight CHECK (score_weight >= 0 AND score_weight <= 1),
  CONSTRAINT chk_rule_severity CHECK (severity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动作评估规则';

-- 用触发器替代 chk_rule_pair_not_self：对称配对规则不允许指向自身
-- （UPDATE 场景是主要防线；INSERT 场景覆盖显式指定主键的情况）
DELIMITER $$
CREATE TRIGGER trg_action_rule_bi BEFORE INSERT ON action_rule FOR EACH ROW
BEGIN
  IF NEW.symmetry_pair_rule_id IS NOT NULL AND NEW.symmetry_pair_rule_id = NEW.action_rule_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'symmetry_pair_rule_id 不能等于自身 action_rule_id';
  END IF;
END$$
CREATE TRIGGER trg_action_rule_bu BEFORE UPDATE ON action_rule FOR EACH ROW
BEGIN
  IF NEW.symmetry_pair_rule_id IS NOT NULL AND NEW.symmetry_pair_rule_id = NEW.action_rule_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'symmetry_pair_rule_id 不能等于自身 action_rule_id';
  END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 4. 文件对象与动作模板
-- ---------------------------------------------------------------------
CREATE TABLE file_object (
  file_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  bucket_name VARCHAR(100) NOT NULL COMMENT 'MinIO存储桶',
  object_key VARCHAR(500) NOT NULL COMMENT '对象键',
  file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  content_type VARCHAR(100) NOT NULL COMMENT 'MIME类型',
  file_size BIGINT UNSIGNED NOT NULL COMMENT '文件大小(字节)',
  sha256 CHAR(64) NOT NULL COMMENT 'SHA-256摘要',
  purpose VARCHAR(30) NOT NULL COMMENT '用途：TEMPLATE样本/ATTACHMENT附件/MESSAGE消息/AVATAR头像',
  scan_status VARCHAR(20) NOT NULL COMMENT '安全扫描状态：PENDING/PASSED/FAILED',
  created_by BIGINT UNSIGNED NOT NULL COMMENT '上传人用户ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  deleted_at DATETIME(3) NULL COMMENT '删除时间（NULL未删）',
  PRIMARY KEY (file_id),
  UNIQUE KEY uk_file_bucket_object (bucket_name, object_key),
  KEY idx_file_purpose (purpose, scan_status),
  CONSTRAINT fk_file_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MinIO文件对象';

CREATE TABLE action_template (
  template_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  action_version_id BIGINT UNSIGNED NOT NULL COMMENT '动作版本ID',
  source_type VARCHAR(20) NOT NULL COMMENT '模板来源：THERAPIST治疗师录制/ALGO算法生成',
  frame_rate DECIMAL(6,2) NOT NULL COMMENT '采样帧率',
  frame_count INT NOT NULL COMMENT '帧数',
  feature_dim INT NOT NULL COMMENT '特征维度',
  feature_sequence JSON NOT NULL COMMENT '标准特征序列',
  keyframe_index JSON NULL COMMENT '关键帧索引',
  sample_count INT NOT NULL COMMENT '采样样本数',
  status VARCHAR(20) NOT NULL COMMENT '状态：ACTIVE/DEPRECATED',
  created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (template_id),
  UNIQUE KEY uk_template_source (action_version_id, source_type),
  CONSTRAINT fk_template_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
  CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标准动作模板';

CREATE TABLE action_template_sample (
  template_sample_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '采样记录ID',
  template_id BIGINT UNSIGNED NOT NULL COMMENT '模板ID',
  file_id BIGINT UNSIGNED NOT NULL COMMENT '样本视频文件ID',
  sample_no INT NOT NULL COMMENT '样本序号',
  feature_hash CHAR(64) NOT NULL COMMENT '特征摘要（去重用）',
  captured_at DATETIME(3) NOT NULL COMMENT '采集时间',
  created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户ID',
  PRIMARY KEY (template_sample_id),
  UNIQUE KEY uk_template_sample (template_id, sample_no),
  CONSTRAINT fk_sample_template FOREIGN KEY (template_id) REFERENCES action_template(template_id),
  CONSTRAINT fk_sample_file FOREIGN KEY (file_id) REFERENCES file_object(file_id),
  CONSTRAINT fk_sample_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动作模板采样记录';

-- ---------------------------------------------------------------------
-- 5. 康复计划与每日任务
-- ---------------------------------------------------------------------
CREATE TABLE rehab_plan (
  plan_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  patient_id BIGINT UNSIGNED NOT NULL COMMENT '患者用户ID',
  therapist_id BIGINT UNSIGNED NOT NULL COMMENT '治疗师用户ID',
  binding_id BIGINT UNSIGNED NOT NULL COMMENT '患者治疗师绑定ID',
  name VARCHAR(100) NOT NULL COMMENT '计划名称',
  rehab_part_code VARCHAR(30) NOT NULL COMMENT '康复部位编码',
  version_no INT NOT NULL COMMENT '计划版本号（调整计划时递增）',
  start_date DATE NOT NULL COMMENT '开始日期',
  end_date DATE NOT NULL COMMENT '结束日期',
  frequency_type VARCHAR(20) NOT NULL COMMENT '频次类型：DAILY每日/WEEKLY每周/CUSTOM自定义',
  frequency_value INT NULL COMMENT '频次值（如每周N次）',
  status VARCHAR(20) NOT NULL COMMENT '状态：ACTIVE/COMPLETED/SUSPENDED/CANCELLED',
  remark TEXT NULL COMMENT '备注',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (plan_id),
  UNIQUE KEY uk_plan_version (patient_id, name, version_no),
  KEY idx_plan_patient_status (patient_id, status),
  KEY idx_plan_binding (binding_id),
  CONSTRAINT fk_plan_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
  CONSTRAINT fk_plan_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id),
  CONSTRAINT fk_plan_part FOREIGN KEY (rehab_part_code) REFERENCES rehab_part_dict(code),
  CONSTRAINT fk_plan_binding_pair FOREIGN KEY (binding_id, patient_id, therapist_id)
    REFERENCES patient_therapist(binding_id, patient_id, therapist_id),
  CONSTRAINT chk_plan_dates CHECK (end_date >= start_date),
  CONSTRAINT chk_plan_version CHECK (version_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='康复计划';

CREATE TABLE plan_item (
  plan_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '计划项ID',
  plan_id BIGINT UNSIGNED NOT NULL COMMENT '计划ID',
  action_version_id BIGINT UNSIGNED NOT NULL COMMENT '动作版本ID',
  sequence_no INT NOT NULL COMMENT '动作顺序号',
  target_sets INT NOT NULL COMMENT '目标组数',
  target_reps INT NULL COMMENT '目标次数/组',
  target_amplitude DECIMAL(6,2) NULL COMMENT '目标幅度(度)',
  duration_sec INT NULL COMMENT '单组时长(秒)',
  rest_sec INT NULL COMMENT '组间休息(秒)',
  instruction TEXT NULL COMMENT '动作指导语',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (plan_item_id),
  UNIQUE KEY uk_plan_item_sequence (plan_id, sequence_no),
  CONSTRAINT fk_item_plan FOREIGN KEY (plan_id) REFERENCES rehab_plan(plan_id),
  CONSTRAINT fk_item_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
  CONSTRAINT chk_item_sequence CHECK (sequence_no > 0),
  CONSTRAINT chk_item_sets CHECK (target_sets > 0),
  CONSTRAINT chk_item_reps CHECK (target_reps IS NULL OR target_reps >= 0),
  CONSTRAINT chk_item_duration CHECK (duration_sec IS NULL OR duration_sec >= 0),
  CONSTRAINT chk_item_rest CHECK (rest_sec IS NULL OR rest_sec >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划动作项';

CREATE TABLE plan_schedule (
  schedule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  plan_id BIGINT UNSIGNED NOT NULL COMMENT '计划ID',
  weekday TINYINT NOT NULL COMMENT '星期几：1周一 ... 7周日',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (schedule_id),
  UNIQUE KEY uk_schedule_day (plan_id, weekday),
  CONSTRAINT fk_schedule_plan FOREIGN KEY (plan_id) REFERENCES rehab_plan(plan_id),
  CONSTRAINT chk_schedule_weekday CHECK (weekday BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划执行日程';

CREATE TABLE plan_daily_task (
  task_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  plan_item_id BIGINT UNSIGNED NOT NULL COMMENT '计划项ID',
  task_date DATE NOT NULL COMMENT '任务日期',
  target_sets INT NOT NULL COMMENT '目标组数（生成任务时快照）',
  target_reps INT NULL COMMENT '目标次数（生成任务时快照）',
  status VARCHAR(20) NOT NULL COMMENT '状态：PENDING/IN_PROGRESS/COMPLETED/SKIPPED/EXPIRED',
  skip_reason VARCHAR(255) NULL COMMENT '跳过原因',
  started_at DATETIME(3) NULL COMMENT '开始时间',
  completed_at DATETIME(3) NULL COMMENT '完成时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (task_id),
  UNIQUE KEY uk_task_item_date (plan_item_id, task_date),
  KEY idx_task_date_status (task_date, status),
  CONSTRAINT fk_task_item FOREIGN KEY (plan_item_id) REFERENCES plan_item(plan_item_id),
  CONSTRAINT chk_task_target_sets CHECK (target_sets > 0),
  CONSTRAINT chk_task_target_reps CHECK (target_reps IS NULL OR target_reps >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日训练任务';

-- ---------------------------------------------------------------------
-- 6. 训练记录与纠错日志
-- ---------------------------------------------------------------------
CREATE TABLE training_record (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  daily_task_id BIGINT UNSIGNED NOT NULL COMMENT '每日任务ID',
  action_version_id BIGINT UNSIGNED NOT NULL COMMENT '动作版本快照（提交时从 plan_item 复制，不随后续版本变更变化）',
  started_at DATETIME(3) NOT NULL COMMENT '训练开始时间',
  ended_at DATETIME(3) NOT NULL COMMENT '训练结束时间',
  total_reps INT NOT NULL COMMENT '总次数',
  correct_reps INT NOT NULL COMMENT '标准次数',
  total_score DECIMAL(5,2) NOT NULL COMMENT '综合得分(0-100)',
  score_detail JSON NOT NULL COMMENT '评分明细（各维度得分）',
  avg_rom DECIMAL(6,2) NULL COMMENT '平均关节活动度(度)',
  peak_angle DECIMAL(6,2) NULL COMMENT '峰值角度(度)',
  trough_angle DECIMAL(6,2) NULL COMMENT '谷值角度(度)',
  peak_frame_no INT NULL COMMENT '峰值帧号',
  trough_frame_no INT NULL COMMENT '谷值帧号',
  hold_ratio DECIMAL(5,4) NULL COMMENT '保持达标率(0-1)',
  template_similarity DECIMAL(5,4) NULL COMMENT '与标准模板相似度(0-1)',
  valid_frame_ratio DECIMAL(5,4) NULL COMMENT '有效帧占比(0-1)',
  pose_repaired_ratio DECIMAL(5,4) NULL COMMENT '关键点修复帧占比(0-1)',
  stability_score DECIMAL(5,2) NULL COMMENT '稳定性得分(0-100)',
  symmetry_score DECIMAL(5,2) NULL COMMENT '对称性得分(0-100)',
  pose_quality VARCHAR(20) NOT NULL COMMENT '姿态质量：GOOD/FAIR/POOR',
  pose_engine_id VARCHAR(150) NOT NULL COMMENT '姿态引擎标识（分析时快照）',
  client_request_id VARCHAR(100) NOT NULL COMMENT '客户端请求ID（幂等防重）',
  remark TEXT NULL COMMENT '备注',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (record_id),
  UNIQUE KEY uk_record_idempotency (daily_task_id, client_request_id),
  KEY idx_record_task_time (daily_task_id, created_at),
  KEY idx_record_version_time (action_version_id, created_at),
  CONSTRAINT fk_record_task FOREIGN KEY (daily_task_id) REFERENCES plan_daily_task(task_id),
  CONSTRAINT fk_record_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
  CONSTRAINT chk_record_time CHECK (ended_at >= started_at),
  CONSTRAINT chk_record_reps CHECK (total_reps >= 0 AND correct_reps BETWEEN 0 AND total_reps),
  CONSTRAINT chk_record_score CHECK (total_score BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练记录';

CREATE TABLE training_error_log (
  error_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '纠错日志ID',
  record_id BIGINT UNSIGNED NOT NULL COMMENT '训练记录ID',
  rule_id BIGINT UNSIGNED NULL COMMENT '触发的规则ID（NULL为非规则类错误）',
  dimension VARCHAR(20) NOT NULL COMMENT '评估维度',
  phase VARCHAR(20) NOT NULL COMMENT '动作阶段',
  error_type VARCHAR(40) NOT NULL COMMENT '错误类型：AMPLITUDE不足/SPEED过快等',
  joint_name VARCHAR(50) NULL COMMENT '关节名称',
  current_value DECIMAL(8,3) NULL COMMENT '实测值',
  expected_value DECIMAL(8,3) NULL COMMENT '期望值',
  deviation DECIMAL(8,3) NULL COMMENT '偏差值',
  severity TINYINT NOT NULL COMMENT '严重等级',
  frame_no INT NULL COMMENT '发生帧号',
  prompt_text VARCHAR(255) NOT NULL COMMENT '纠错提示文案（快照）',
  error_at DATETIME(3) NOT NULL COMMENT '错误发生时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (error_id),
  KEY idx_error_record (record_id),
  KEY idx_error_dimension (dimension, severity),
  CONSTRAINT fk_error_record FOREIGN KEY (record_id) REFERENCES training_record(record_id),
  CONSTRAINT fk_error_rule FOREIGN KEY (rule_id) REFERENCES action_rule(action_rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练纠错日志';

-- ---------------------------------------------------------------------
-- 7. 评估报告
-- ---------------------------------------------------------------------
CREATE TABLE assessment_report (
  report_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  plan_id BIGINT UNSIGNED NOT NULL COMMENT '计划ID',
  period_start DATE NOT NULL COMMENT '统计周期开始',
  period_end DATE NOT NULL COMMENT '统计周期结束',
  data_snapshot JSON NOT NULL COMMENT '报告数据快照（指标聚合结果）',
  ai_draft TEXT NULL COMMENT 'AI生成的报告草稿',
  therapist_conclusion TEXT NULL COMMENT '治疗师审核结论',
  status VARCHAR(20) NOT NULL COMMENT '状态：DRAFT/AI_GENERATED/REVIEWED/PUBLISHED',
  reviewed_by BIGINT UNSIGNED NULL COMMENT '审核治疗师用户ID',
  reviewed_at DATETIME(3) NULL COMMENT '审核时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (report_id),
  UNIQUE KEY uk_report_period (plan_id, period_start, period_end),
  CONSTRAINT fk_report_plan FOREIGN KEY (plan_id) REFERENCES rehab_plan(plan_id),
  CONSTRAINT fk_report_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account(user_id),
  CONSTRAINT chk_report_period CHECK (period_end >= period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评估报告';

-- ---------------------------------------------------------------------
-- 8. 健康内容（科普 / 收藏 / 附件）
-- ---------------------------------------------------------------------
CREATE TABLE content (
  content_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  content_type VARCHAR(30) NOT NULL COMMENT '内容类型：ARTICLE文章/VIDEO视频/GUIDE指南',
  target_part_code VARCHAR(30) NOT NULL COMMENT '关联康复部位编码',
  target_group VARCHAR(100) NULL COMMENT '目标人群标签',
  summary VARCHAR(500) NULL COMMENT '摘要',
  content_body LONGTEXT NOT NULL COMMENT '正文',
  author_id BIGINT UNSIGNED NOT NULL COMMENT '作者用户ID',
  status VARCHAR(20) NOT NULL COMMENT '状态：DRAFT/PENDING_REVIEW/PUBLISHED/OFFLINE',
  reviewer_id BIGINT UNSIGNED NULL COMMENT '审核人用户ID',
  review_remark VARCHAR(500) NULL COMMENT '审核意见',
  valid_from DATETIME(3) NOT NULL COMMENT '生效时间',
  valid_to DATETIME(3) NULL COMMENT '失效时间',
  published_at DATETIME(3) NULL COMMENT '发布时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (content_id),
  KEY idx_content_filter (status, target_part_code, valid_from, valid_to),
  CONSTRAINT fk_content_part FOREIGN KEY (target_part_code) REFERENCES rehab_part_dict(code),
  CONSTRAINT fk_content_author FOREIGN KEY (author_id) REFERENCES user_account(user_id),
  CONSTRAINT fk_content_reviewer FOREIGN KEY (reviewer_id) REFERENCES user_account(user_id),
  CONSTRAINT chk_content_valid_period CHECK (valid_to IS NULL OR valid_to >= valid_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公共健康内容';

CREATE TABLE content_attachment (
  content_attachment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '附件关联ID',
  content_id BIGINT UNSIGNED NOT NULL COMMENT '内容ID',
  file_id BIGINT UNSIGNED NOT NULL COMMENT '文件ID',
  attachment_type VARCHAR(20) NOT NULL COMMENT '附件类型：COVER封面/VIDEO视频/IMAGE图片/DOC文档',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (content_attachment_id),
  UNIQUE KEY uk_content_file (content_id, file_id),
  CONSTRAINT fk_ca_content FOREIGN KEY (content_id) REFERENCES content(content_id),
  CONSTRAINT fk_ca_file FOREIGN KEY (file_id) REFERENCES file_object(file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容附件';

CREATE TABLE content_collection (
  collection_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  content_id BIGINT UNSIGNED NOT NULL COMMENT '内容ID',
  collected_at DATETIME(3) NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (collection_id),
  UNIQUE KEY uk_collection_user_content (user_id, content_id),
  CONSTRAINT fk_collection_user FOREIGN KEY (user_id) REFERENCES user_account(user_id),
  CONSTRAINT fk_collection_content FOREIGN KEY (content_id) REFERENCES content(content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容收藏';

-- ---------------------------------------------------------------------
-- 9. 咨询会话与消息
-- ---------------------------------------------------------------------
CREATE TABLE consultation (
  consultation_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  patient_id BIGINT UNSIGNED NOT NULL COMMENT '患者用户ID',
  therapist_id BIGINT UNSIGNED NOT NULL COMMENT '治疗师用户ID',
  binding_id BIGINT UNSIGNED NOT NULL COMMENT '患者治疗师绑定ID',
  status VARCHAR(20) NOT NULL COMMENT '状态：OPEN/CLOSED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  closed_at DATETIME(3) NULL COMMENT '关闭时间',
  PRIMARY KEY (consultation_id),
  KEY idx_consult_patient (patient_id, status),
  KEY idx_consult_therapist (therapist_id, status),
  KEY idx_consult_binding (binding_id),
  CONSTRAINT fk_consult_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
  CONSTRAINT fk_consult_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id),
  CONSTRAINT fk_consult_binding_pair FOREIGN KEY (binding_id, patient_id, therapist_id)
    REFERENCES patient_therapist(binding_id, patient_id, therapist_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咨询会话';

CREATE TABLE consultation_message (
  message_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  consultation_id BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  sender_id BIGINT UNSIGNED NOT NULL COMMENT '发送人用户ID',
  content TEXT NULL COMMENT '文本内容（纯附件消息可为空）',
  is_ai_draft TINYINT NOT NULL DEFAULT 0 COMMENT '是否AI草稿 1是0否',
  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读 1是0否',
  read_at DATETIME(3) NULL COMMENT '阅读时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发送时间',
  PRIMARY KEY (message_id),
  KEY idx_message_consult_time (consultation_id, created_at),
  KEY idx_message_unread (sender_id, is_read),
  CONSTRAINT fk_message_consult FOREIGN KEY (consultation_id) REFERENCES consultation(consultation_id),
  CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咨询消息';

CREATE TABLE consultation_attachment (
  consultation_attachment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息附件ID',
  message_id BIGINT UNSIGNED NOT NULL COMMENT '消息ID',
  file_id BIGINT UNSIGNED NOT NULL COMMENT '文件ID',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (consultation_attachment_id),
  UNIQUE KEY uk_message_file (message_id, file_id),
  CONSTRAINT fk_ma_message FOREIGN KEY (message_id) REFERENCES consultation_message(message_id),
  CONSTRAINT fk_ma_file FOREIGN KEY (file_id) REFERENCES file_object(file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咨询消息附件';

-- ---------------------------------------------------------------------
-- 10. 审计日志与公告
-- ---------------------------------------------------------------------
CREATE TABLE operation_log (
  log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  user_id BIGINT UNSIGNED NULL COMMENT '操作人用户ID（NULL为系统操作）',
  module VARCHAR(30) NOT NULL COMMENT '模块名',
  action VARCHAR(50) NOT NULL COMMENT '操作类型',
  target_type VARCHAR(50) NOT NULL COMMENT '目标对象类型',
  target_id BIGINT UNSIGNED NULL COMMENT '目标对象ID',
  ip VARCHAR(45) NOT NULL COMMENT '客户端IP（兼容IPv6）',
  device_id VARCHAR(100) NULL COMMENT '设备标识',
  status VARCHAR(20) NOT NULL COMMENT '结果：SUCCESS/FAIL',
  detail JSON NULL COMMENT '操作详情（参数、变更前后值等）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
  PRIMARY KEY (log_id),
  KEY idx_log_user_time (user_id, created_at),
  KEY idx_log_module_time (module, created_at),
  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES user_account(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志';

CREATE TABLE announcement (
  announcement_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  content LONGTEXT NOT NULL COMMENT '公告正文',
  scope VARCHAR(20) NOT NULL COMMENT '可见范围：ALL全部/PATIENT患者/THERAPIST治疗师',
  status VARCHAR(20) NOT NULL COMMENT '状态：DRAFT/PUBLISHED/OFFLINE',
  start_time DATETIME(3) NOT NULL COMMENT '展示开始时间',
  end_time DATETIME(3) NOT NULL COMMENT '展示结束时间',
  created_by BIGINT UNSIGNED NOT NULL COMMENT '创建人用户ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常1已删',
  PRIMARY KEY (announcement_id),
  KEY idx_notice_scope_status (scope, status, start_time, end_time),
  CONSTRAINT fk_notice_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id),
  CONSTRAINT chk_notice_period CHECK (end_time >= start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台公告';

-- ---------------------------------------------------------------------
-- 11. 种子数据
-- ---------------------------------------------------------------------
-- 康复部位字典（可按实际运营内容扩展）
-- 大类：UPPER 上肢 / LOWER 下肢 / SPINE 脊柱 / OVERALL 全身综合
-- 不收录手指、足趾、面部（MoveNet 17 点无对应关键点）
INSERT IGNORE INTO rehab_part_dict (code, name, group_code, sort_no, enabled) VALUES
 ('SHOULDER', '肩部',     'UPPER',   10, 1),
 ('ELBOW',    '肘部',     'UPPER',   20, 1),
 ('WRIST',    '腕部',     'UPPER',   30, 1),
 ('HIP',      '髋部',     'LOWER',   40, 1),
 ('KNEE',     '膝部',     'LOWER',   50, 1),
 ('ANKLE',    '踝部',     'LOWER',   60, 1),
 ('CERVICAL', '颈椎',     'SPINE',   70, 1),
 ('LUMBAR',   '腰椎',     'SPINE',   80, 1),
 ('OVERALL',  '全身综合', 'OVERALL', 90, 1);

-- 初始管理员账号（账号 admin，临时密码 password，BCrypt 密文，首次登录后必须修改）
-- 显式指定 user_id=1，后续自增 ID 自动从 2 开始
INSERT IGNORE INTO user_account (user_id, username, password_hash, role_code, phone, status, deleted)
VALUES (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', NULL, 1, 0);
