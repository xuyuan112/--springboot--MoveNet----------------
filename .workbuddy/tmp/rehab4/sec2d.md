表23　咨询会话（consultation / Consultation）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| consultation_id | BIGINT UNSIGNED | 主键，自增 | 会话 ID |
| patient_id | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 患者用户 ID |
| therapist_id | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 治疗师用户 ID |
| binding_id | BIGINT UNSIGNED | 非空，复合外键 (binding_id, patient_id, therapist_id)→patient_therapist | 绑定关系，创建会话时必须有有效绑定 |
| status | VARCHAR(20) | 非空 | 状态 OPEN 进行中 / CLOSED 已关闭 |
| created_at | DATETIME(3) | 非空 | 创建时间 |
| closed_at | DATETIME(3) | 可空 | 关闭时间 |
| 索引 | — | idx_consult_patient、idx_consult_therapist、idx_consult_binding | 按双方与绑定检索会话 |

表24　咨询消息（consultation_message / ConsultationMessage）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| message_id | BIGINT UNSIGNED | 主键，自增 | 消息 ID |
| consultation_id | BIGINT UNSIGNED | 非空，外键→consultation.consultation_id | 所属会话 |
| sender_id | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 发送人（必须属于会话双方） |
| content | TEXT | 可空 | 文本内容（纯附件消息可为空） |
| is_ai_draft | TINYINT | 非空，默认 0 | 是否 AI 草稿（不可直接展示或发送） |
| is_read | TINYINT | 非空，默认 0 | 是否已读 |
| read_at | DATETIME(3) | 可空 | 阅读时间 |
| created_at | DATETIME(3) | 非空 | 发送时间 |
| 索引 | — | idx_message_consult_time(consultation_id, created_at)、idx_message_unread(sender_id, is_read) | 会话消息分页与未读统计 |

表25　消息附件（consultation_attachment / ConsultationAttachment）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| consultation_attachment_id | BIGINT UNSIGNED | 主键，自增 | 附件关联 ID |
| message_id | BIGINT UNSIGNED | 非空，外键→consultation_message.message_id | 所属消息 |
| file_id | BIGINT UNSIGNED | 非空，外键→file_object.file_id，组合唯一 uk_message_file(message_id, file_id) | MinIO 文件 |
| sort_no | INT | 非空，默认 0 | 展示顺序 |
| created_at | DATETIME(3) | 非空 | 创建时间 |

表26　文件对象（file_object / FileObject）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| file_id | BIGINT UNSIGNED | 主键，自增 | 文件 ID |
| bucket_name | VARCHAR(100) | 非空，组合唯一 uk_file_bucket_object(bucket_name, object_key) | MinIO 存储桶 |
| object_key | VARCHAR(500) | 非空 | 对象键 |
| file_name | VARCHAR(255) | 非空 | 原始文件名 |
| content_type | VARCHAR(100) | 非空 | MIME 类型 |
| file_size | BIGINT UNSIGNED | 非空 | 文件大小（字节） |
| sha256 | CHAR(64) | 非空 | 文件 SHA-256 摘要 |
| purpose | VARCHAR(30) | 非空 | 用途 TEMPLATE 样本 / ATTACHMENT 附件 / MESSAGE 消息 / AVATAR 头像 |
| scan_status | VARCHAR(20) | 非空 | 安全扫描状态 PENDING/PASSED/FAILED |
| created_by | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 上传人 |
| created_at | DATETIME(3) | 非空 | 创建时间 |
| deleted_at | DATETIME(3) | 可空 | 删除时间（NULL 表示未删除） |
| 索引 | — | idx_file_purpose(purpose, scan_status) | 按用途与扫描状态检索 |

表27　操作日志（operation_log / OperationLog）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| log_id | BIGINT UNSIGNED | 主键，自增 | 日志 ID |
| user_id | BIGINT UNSIGNED | 可空，外键→user_account.user_id | 操作人（NULL 为系统操作） |
| module | VARCHAR(30) | 非空 | 模块名 |
| action | VARCHAR(50) | 非空 | 操作类型 |
| target_type | VARCHAR(50) | 非空 | 目标对象类型（多态目标，不设外键） |
| target_id | BIGINT UNSIGNED | 可空 | 目标对象 ID |
| ip | VARCHAR(45) | 非空 | 客户端 IP（兼容 IPv6） |
| device_id | VARCHAR(100) | 可空 | 设备标识 |
| status | VARCHAR(20) | 非空 | 结果 SUCCESS/FAIL |
| detail | JSON | 可空 | 操作详情（参数、变更前后值等） |
| created_at | DATETIME(3) | 非空 | 操作时间 |
| 索引 | — | idx_log_user_time(user_id, created_at)、idx_log_module_time(module, created_at) | 按人与按模块审计 |

表28　公告（announcement / Announcement）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| announcement_id | BIGINT UNSIGNED | 主键，自增 | 公告 ID |
| title | VARCHAR(200) | 非空 | 公告标题 |
| content | LONGTEXT | 非空 | 公告正文 |
| scope | VARCHAR(20) | 非空 | 可见范围 ALL 全部 / PATIENT 患者 / THERAPIST 治疗师 |
| status | VARCHAR(20) | 非空 | DRAFT/PUBLISHED/OFFLINE |
| start_time / end_time | DATETIME(3) | 非空，CHECK end_time 不早于 start_time | 展示时间范围 |
| created_by | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 发布管理员 |
| created_at / updated_at / deleted | DATETIME(3) / TINYINT | 非空 | 审计字段 |
| 索引 | — | idx_notice_scope_status(scope, status, start_time, end_time) | 按可见范围与状态检索 |
