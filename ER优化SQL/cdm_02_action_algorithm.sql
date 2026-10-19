-- ============================================================
-- CDM-02 动作库与姿态评估算法（Action & Algorithm）
-- 用途：PowerDesigner 反向工程 -> 生成 CDM-02 PDM / CDM
-- 依据：er图分模块建议.md 模块二
--
-- 包含 7 张核心表：
--   rehab_part_dict          康复部位字典（共享基础数据）
--   action                   标准动作
--   action_version           动作版本
--   action_rule              动作评估规则（含自关联对称配对）
--   action_template          标准动作模板
--   action_template_sample   模板采样记录
--   file_object              MinIO 文件对象（共享实体）
--
-- 跨模块外部引用（字段保留，不建物理外键）：
--   action.created_by           -> user_account（见 CDM-01）
--   action_version.created_by   -> user_account（见 CDM-01）
--   action_template.created_by  -> user_account（见 CDM-01）
--   action_template_sample.created_by -> user_account（见 CDM-01）
--   file_object.created_by      -> user_account（见 CDM-01）
--
-- 不包含：user_account（见 CDM-01）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ---------- 1. 康复部位字典（共享基础数据） ----------
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

-- ---------- 2. 标准动作 ----------
CREATE TABLE action (
    action_id BIGINT NOT NULL COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    target_part_code VARCHAR(30) NOT NULL COMMENT '目标部位编码',
    difficulty VARCHAR(20) NOT NULL COMMENT '动作难度',
    description TEXT NULL COMMENT '动作描述',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    created_by BIGINT NOT NULL COMMENT '创建人ID（外部引用 user_account，见 CDM-01）',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (action_id),
    KEY idx_action_part_status (target_part_code,status),
    CONSTRAINT fk_action_part FOREIGN KEY (target_part_code) REFERENCES rehab_part_dict(code),
    CONSTRAINT chk_action_deleted CHECK (deleted IN (0,1))
) COMMENT='标准动作';

-- ---------- 3. 动作版本 ----------
CREATE TABLE action_version (
    action_version_id BIGINT NOT NULL COMMENT '主键ID',
    action_id BIGINT NOT NULL COMMENT '标准动作ID',
    version_no INT NOT NULL COMMENT '版本号',
    action_type VARCHAR(20) NOT NULL COMMENT '动作类型 COUNTABLE/HOLDABLE/TIMED',
    algo_config JSON NOT NULL COMMENT '算法参数快照',
    motion_signature JSON NOT NULL COMMENT '动作签名',
    pose_engine_id VARCHAR(150) NOT NULL COMMENT '姿态引擎版本',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    created_by BIGINT NOT NULL COMMENT '创建人ID（外部引用 user_account，见 CDM-01）',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (action_version_id),
    UNIQUE KEY uk_action_version (action_id,version_no),
    KEY idx_action_version_status (action_id,status),
    CONSTRAINT fk_av_action FOREIGN KEY (action_id) REFERENCES action(action_id)
) COMMENT='动作版本';

-- ---------- 4. 动作评估规则（symmetry_pair_rule_id 自关联） ----------
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
    symmetry_pair_rule_id BIGINT NULL COMMENT '对称配对规则ID（必须属于同一动作版本）',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (action_rule_id),
    UNIQUE KEY uk_rule_dimension (action_version_id,joint_name,dimension),
    UNIQUE KEY uk_rule_id_version (action_rule_id,action_version_id),
    CONSTRAINT fk_rule_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id),
    -- 自关联：对称配对规则必须属于同一动作版本
    CONSTRAINT fk_rule_pair FOREIGN KEY (symmetry_pair_rule_id,action_version_id)
        REFERENCES action_rule(action_rule_id,action_version_id),
    CONSTRAINT chk_rule_pair_not_self CHECK (symmetry_pair_rule_id IS NULL OR symmetry_pair_rule_id <> action_rule_id),
    CONSTRAINT chk_rule_weight CHECK (score_weight >= 0 AND score_weight <= 1),
    CONSTRAINT chk_rule_severity CHECK (severity >= 0)
) COMMENT='动作评估规则';

-- ---------- 5. 标准动作模板 ----------
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
    created_by BIGINT NOT NULL COMMENT '创建人ID（外部引用 user_account，见 CDM-01）',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (template_id),
    UNIQUE KEY uk_template_source (action_version_id,source_type),
    CONSTRAINT fk_template_version FOREIGN KEY (action_version_id) REFERENCES action_version(action_version_id)
) COMMENT='标准动作模板';

-- ---------- 6. MinIO 文件对象（共享实体） ----------
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
    created_by BIGINT NOT NULL COMMENT '创建人ID（外部引用 user_account，见 CDM-01）',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    PRIMARY KEY (file_id),
    UNIQUE KEY uk_file_bucket_object (bucket_name,object_key),
    KEY idx_file_purpose (purpose,scan_status)
) COMMENT='MinIO文件对象';

-- ---------- 7. 动作模板采样记录 ----------
CREATE TABLE action_template_sample (
    template_sample_id BIGINT NOT NULL COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    file_id BIGINT NOT NULL COMMENT '文件对象ID',
    sample_no INT NOT NULL COMMENT '采样序号',
    feature_hash CHAR(64) NOT NULL COMMENT '特征哈希',
    captured_at DATETIME NOT NULL COMMENT '采集时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID（外部引用 user_account，见 CDM-01）',
    PRIMARY KEY (template_sample_id),
    UNIQUE KEY uk_template_sample (template_id,sample_no),
    CONSTRAINT fk_sample_template FOREIGN KEY (template_id) REFERENCES action_template(template_id),
    CONSTRAINT fk_sample_file FOREIGN KEY (file_id) REFERENCES file_object(file_id)
) COMMENT='动作模板采样记录';

-- ---------- 自增主键（rehab_part_dict.code 为业务编码，不自增） ----------
ALTER TABLE action MODIFY action_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_version MODIFY action_version_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_rule MODIFY action_rule_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_template MODIFY template_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE file_object MODIFY file_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE action_template_sample MODIFY template_sample_id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS=1;
