-- ============================================================
-- CDM-06 公共内容与文件资源（Content & File）
-- 用途：PowerDesigner 反向工程 -> 生成 CDM-06 PDM / CDM
-- 依据：er图分模块建议.md 模块六
--
-- 本模块核心 3 张表：
--   content              科普/训练指导内容
--   content_attachment   内容附件
--   content_collection   用户内容收藏
--
-- 共享基础实体 3 张（图上用浅色/虚线标识）：
--   user_account         用户账号（见 CDM-01）
--   rehab_part_dict      康复部位字典（见 CDM-02）
--   file_object          MinIO文件对象（见 CDM-02）
--
-- 本模块内所有外键均闭环，无跨模块物理外键断开
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

-- ---------- 2. 康复部位字典（共享基础数据，见 CDM-02） ----------
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

-- ---------- 4. 科普/训练指导内容（本模块核心，字段与主表全量对齐） ----------
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

-- ---------- 5. 内容附件 ----------
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

-- ---------- 6. 用户内容收藏 ----------
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

-- ---------- 自增主键（rehab_part_dict.code 为业务编码，不自增） ----------
ALTER TABLE user_account MODIFY user_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE file_object MODIFY file_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE content MODIFY content_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE content_attachment MODIFY content_attachment_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE content_collection MODIFY collection_id BIGINT NOT NULL AUTO_INCREMENT;

SET FOREIGN_KEY_CHECKS=1;
