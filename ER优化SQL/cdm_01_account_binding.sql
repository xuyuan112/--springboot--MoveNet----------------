-- ============================================================
-- CDM-01 账户、患者与治疗师关系（Account & Binding）
-- 用途：PowerDesigner 反向工程 -> 生成 CDM-01 PDM / CDM
-- 依据：er图分模块建议.md 模块一
--
-- 包含 5 张核心表：
--   user_account            用户账号（所有外键根）
--   patient_profile         患者档案
--   therapist_profile       治疗师档案
--   patient_therapist       患者-治疗师负责关系
--   patient_rehab_part      患者康复部位（多对多，含侧别）
--
-- 跨模块外部引用（字段保留，不建物理外键）：
--   patient_rehab_part.rehab_part_code -> rehab_part_dict（共享基础数据域）
--
-- 不包含：rehab_part_dict（见共享基础数据 / CDM-02 / CDM-06）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ---------- 1. 用户账号 ----------
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

-- ---------- 2. 患者档案（已去除单一康复部位字段，部位见 patient_rehab_part） ----------
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

-- ---------- 3. 患者康复部位（多对多，侧别 side） ----------
CREATE TABLE patient_rehab_part (
    patient_rehab_part_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID(user_account.id)',
    rehab_part_code VARCHAR(30) NOT NULL COMMENT '康复部位编码（外部引用 rehab_part_dict，见共享基础数据域）',
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
    CONSTRAINT chk_pp_side CHECK (side IN ('LEFT','RIGHT','BILATERAL')),
    CONSTRAINT chk_pp_primary CHECK (primary_flag IN (0,1))
) COMMENT='患者康复部位（多对多）';

-- ---------- 4. 治疗师档案 ----------
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

-- ---------- 5. 患者-治疗师负责关系 ----------
-- 注意：patient_id/therapist_id 引用的是 user_account.id（用户主体），不是 profile.id
-- 角色合法性（PATIENT/THERAPIST）由服务层校验，普通外键无法表达跨表角色约束
CREATE TABLE patient_therapist (
    binding_id BIGINT NOT NULL COMMENT '主键ID',
    patient_id BIGINT NOT NULL COMMENT '患者用户ID',
    therapist_id BIGINT NOT NULL COMMENT '治疗师用户ID',
    relation_type VARCHAR(20) NOT NULL COMMENT '关系类型 PRIMARY/ASSISTANT',
    status VARCHAR(20) NOT NULL COMMENT '状态 ACTIVE/ENDED',
    start_at DATETIME NOT NULL COMMENT '生效时间',
    end_at DATETIME NULL COMMENT '结束时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (binding_id),
    -- 复合唯一键，供 rehab_plan / consultation 的 binding_id 复合外键引用（见 CDM-03/CDM-07）
    UNIQUE KEY uk_binding_id_pair (binding_id, patient_id, therapist_id),
    KEY idx_binding_patient (patient_id,status),
    KEY idx_binding_therapist (therapist_id,status),
    CONSTRAINT fk_binding_patient FOREIGN KEY (patient_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_binding_therapist FOREIGN KEY (therapist_id) REFERENCES user_account(user_id),
    CONSTRAINT fk_binding_creator FOREIGN KEY (created_by) REFERENCES user_account(user_id),
    CONSTRAINT chk_binding_period CHECK (end_at IS NULL OR end_at >= start_at)
) COMMENT='患者治疗师关系';

-- ---------- 自增主键 ----------
ALTER TABLE user_account MODIFY user_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE patient_profile MODIFY patient_profile_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE patient_rehab_part MODIFY patient_rehab_part_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE therapist_profile MODIFY therapist_profile_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE patient_therapist MODIFY binding_id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS=1;
