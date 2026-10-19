-- ============================================================
-- CDM-07 咨询、审计与公告（Consultation & Ops）
-- 用途：PowerDesigner 反向工程 -> 生成 CDM-07 PDM / CDM
-- 依据：er图分模块建议.md 模块七
--
-- 本模块核心 5 张表：
--   consultation            医患在线咨询
--   consultation_message    咨询消息
--   consultation_attachment 咨询消息附件
--   operation_log           平台操作审计日志
--   announcement            平台公告
--
-- 共享实体 3 张（图上用浅色/虚线标识）：
--   user_account            用户账号（见 CDM-01）
--   patient_therapist       患者-治疗师绑定（见 CDM-01）
--   file_object             MinIO文件对象（见 CDM-02）
--
-- 本模块内所有外键均闭环
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

-- ---------- 2. 患者-治疗师绑定（外部引用实体，见 CDM-01） ----------
-- 本文件仅保留 consultation 复合外键所需的最小结构：binding_id/patient_id/therapist_id + uk_binding_id_pair
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

-- ---------- 3. MinIO 文件对象（共享实体，见 CDM-02） ----------
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

-- ---------- 4. 医患在线咨询（本模块核心，字段与主表全量对齐） ----------
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
    -- 复合外键：保证 patient_id/therapist_id 与绑定记录一致
    CONSTRAINT fk_consult_binding_pair FOREIGN KEY (binding_id, patient_id, therapist_id)
        REFERENCES patient_therapist (binding_id, patient_id, therapist_id)
) COMMENT='咨询会话';

-- ---------- 5. 咨询消息 ----------
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

-- ---------- 6. 咨询消息附件（挂在消息下，字段与主表全量对齐） ----------
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

-- ---------- 7. 平台操作审计日志（字段与主表全量对齐） ----------
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

-- ---------- 8. 平台公告（字段与主表全量对齐） ----------
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

-- ---------- 自增主键 ----------
ALTER TABLE user_account MODIFY user_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE patient_therapist MODIFY binding_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE file_object MODIFY file_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE consultation MODIFY consultation_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE consultation_message MODIFY message_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE consultation_attachment MODIFY consultation_attachment_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE operation_log MODIFY log_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE announcement MODIFY announcement_id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS=1;
