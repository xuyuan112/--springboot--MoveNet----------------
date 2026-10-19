将图1的概念模型按“一个实体转换为一张表、一个多对多联系转换为一张关联表”的规则转换为关系模式，共得到 27 张数据表，清单见表1。物理实现遵循以下约定：数据库为 MySQL 8.0，存储引擎 InnoDB，字符集 utf8mb4 / utf8mb4_unicode_ci；所有数值型主键统一命名为“表前缀_id”，类型为 BIGINT UNSIGNED 并自增，应用层可不传主键；业务表统一携带 created_at、updated_at、deleted 三个审计字段，其中时间字段使用 DATETIME(3)（毫秒精度）并由数据库默认值与自动更新维护；角度、比例等数值一律使用 DECIMAL 而不使用浮点类型，避免二进制误差影响评分；算法参数、动作签名、评分明细与报告快照使用 JSON 列保存不可变快照，且不作为可检索业务字段使用；角色取值、状态取值、日期区间、评分区间与目标值等业务规则通过 CHECK 约束在数据库层兜底，患者与治疗师的绑定一致性通过复合外键保证。各表的字段、数据类型与约束条件见表2至表28。

表1　数据表清单

| 序号 | 中文表名 | 英文表名 | 实体（Entity） | 用途 |
| --- | --- | --- | --- | --- |
| 1 | 用户账号 | user_account | UserAccount | 登录主体和角色 |
| 2 | 患者档案 | patient_profile | PatientProfile | 患者基本健康资料 |
| 3 | 治疗师档案 | therapist_profile | TherapistProfile | 治疗师资质资料 |
| 4 | 患者-治疗师关系 | patient_therapist | PatientTherapist | 有效负责关系（绑定） |
| 5 | 患者康复部位 | patient_rehab_part | PatientRehabPart | 患者多部位康复记录（含侧别、主部位） |
| 6 | 康复部位字典 | rehab_part_dict | RehabPart | 部位编码与大类 |
| 7 | 标准动作 | action | Action | 动作主数据 |
| 8 | 动作版本 | action_version | ActionVersion | 不可变评估版本 |
| 9 | 动作规则 | action_rule | ActionRule | 关节、阈值与评分规则 |
| 10 | 标准动作模板 | action_template | ActionTemplate | DTW 特征模板 |
| 11 | 模板采样记录 | action_template_sample | ActionTemplateSample | 模板示范样本元数据 |
| 12 | 康复计划 | rehab_plan | RehabPlan | 患者治疗方案 |
| 13 | 计划动作项 | plan_item | PlanItem | 计划内动作及目标 |
| 14 | 计划执行日程 | plan_schedule | PlanSchedule | 每周执行星期 |
| 15 | 每日训练任务 | plan_daily_task | PlanDailyTask | 动作项 × 日期实例 |
| 16 | 训练记录 | training_record | TrainingRecord | 一次训练聚合结果 |
| 17 | 训练纠错日志 | training_error_log | TrainingErrorLog | 规则级纠错摘要 |
| 18 | 评估报告 | assessment_report | AssessmentReport | 周期汇总与审核结果 |
| 19 | 公共内容 | content | Content | 健康建议、动作说明 |
| 20 | 内容附件 | content_attachment | ContentAttachment | MinIO 对象关联 |
| 21 | 内容收藏 | content_collection | ContentCollection | 用户收藏关系 |
| 22 | 咨询会话 | consultation | Consultation | 患者与治疗师会话 |
| 23 | 咨询消息 | consultation_message | ConsultationMessage | 会话消息 |
| 24 | 消息附件 | consultation_attachment | ConsultationAttachment | 受控资料 MinIO 关联 |
| 25 | 文件对象 | file_object | FileObject | MinIO 对象元数据 |
| 26 | 操作日志 | operation_log | OperationLog | 审计和登录日志 |
| 27 | 公告 | announcement | Announcement | 平台公告 |

表2　用户账号（user_account / UserAccount）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| user_id | BIGINT UNSIGNED | 主键，自增 | 用户账号 ID |
| username | VARCHAR(50) | 非空，唯一 | 登录名 |
| password_hash | VARCHAR(255) | 非空 | BCrypt 密码密文 |
| role_code | VARCHAR(20) | 非空，CHECK 取值 PATIENT/THERAPIST/ADMIN | 角色编码 |
| phone | VARCHAR(20) | 可空，唯一 | 手机号 |
| status | TINYINT | 非空，默认 1，CHECK 取值 0/1 | 1 启用、0 禁用 |
| last_login_at | DATETIME(3) | 可空 | 最后登录时间 |
| created_at | DATETIME(3) | 非空，默认当前时间 | 创建时间 |
| updated_at | DATETIME(3) | 非空，更新时自动刷新 | 更新时间 |
| deleted | TINYINT | 非空，默认 0，CHECK 取值 0/1 | 逻辑删除标记 |

表3　患者档案（patient_profile / PatientProfile）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| patient_profile_id | BIGINT UNSIGNED | 主键，自增 | 患者档案 ID |
| user_id | BIGINT UNSIGNED | 非空，唯一，外键→user_account.user_id | 对应用户账号，实现 1:1 |
| real_name | VARCHAR(50) | 非空 | 真实姓名 |
| gender | CHAR(1) | 非空，CHECK 取值 M/F/U | 性别 |
| birth_date | DATE | 非空 | 出生日期（年龄由此计算，不落库） |
| height_cm | DECIMAL(5,2) | 可空，CHECK 大于 0 | 身高（厘米） |
| weight_kg | DECIMAL(5,2) | 可空，CHECK 大于 0 | 体重（千克） |
| medical_history | TEXT | 可空 | 既往病史 |
| allergy_info | TEXT | 可空 | 过敏史 |
| contraindication | TEXT | 可空 | 禁忌症 |
| created_at / updated_at | DATETIME(3) | 非空 | 创建与更新时间 |
| deleted | TINYINT | 非空，默认 0 | 逻辑删除标记 |

表4　治疗师档案（therapist_profile / TherapistProfile）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| therapist_profile_id | BIGINT UNSIGNED | 主键，自增 | 治疗师档案 ID |
| user_id | BIGINT UNSIGNED | 非空，唯一，外键→user_account.user_id | 对应用户账号，实现 1:1 |
| real_name | VARCHAR(50) | 非空 | 真实姓名 |
| title | VARCHAR(50) | 非空 | 职称 |
| specialty | VARCHAR(100) | 非空 | 专长领域 |
| license_no | VARCHAR(100) | 可空，唯一 | 执业证书编号 |
| introduction | TEXT | 可空 | 个人简介 |
| created_at / updated_at | DATETIME(3) | 非空 | 创建与更新时间 |
| deleted | TINYINT | 非空，默认 0 | 逻辑删除标记 |

表5　患者-治疗师关系（patient_therapist / PatientTherapist）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| binding_id | BIGINT UNSIGNED | 主键，自增 | 绑定关系 ID |
| patient_id | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 患者用户 ID |
| therapist_id | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 治疗师用户 ID |
| relation_type | VARCHAR(20) | 非空 | 关系类型 PRIMARY 主治 / SECONDARY 协作 |
| status | VARCHAR(20) | 非空 | 状态 ACTIVE 有效 / ENDED 已结束 |
| start_at | DATETIME(3) | 非空 | 绑定开始时间 |
| end_at | DATETIME(3) | 可空，CHECK end_at 不早于 start_at | 绑定结束时间 |
| created_by | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 创建人（管理员） |
| created_at | DATETIME(3) | 非空 | 创建时间 |
| 组合唯一键 | — | uk_binding_id_pair(binding_id, patient_id, therapist_id) | 供康复计划、咨询会话的复合外键引用 |
| 索引 | — | idx_binding_patient(patient_id,status)、idx_binding_therapist(therapist_id,status) | 按患者、治疗师查询有效绑定 |

表6　患者康复部位（patient_rehab_part / PatientRehabPart）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| patient_rehab_part_id | BIGINT UNSIGNED | 主键，自增 | 登记 ID |
| patient_id | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 患者用户 ID |
| rehab_part_code | VARCHAR(30) | 非空，外键→rehab_part_dict.code | 康复部位编码 |
| side | VARCHAR(10) | 非空，默认 BILATERAL，CHECK 取值 LEFT/RIGHT/BILATERAL | 侧别 |
| primary_flag | TINYINT | 非空，默认 0，CHECK 取值 0/1 | 是否主康复部位（服务层保证同一患者至多一条为 1） |
| status | VARCHAR(20) | 非空，默认 ACTIVE | 状态 ACTIVE 康复中 / RESOLVED 已结束 |
| diagnosed_at | DATE | 可空 | 确诊或受损日期 |
| remark | VARCHAR(255) | 可空 | 备注 |
| created_at / updated_at | DATETIME(3) | 非空 | 创建与更新时间 |
| deleted | TINYINT | 非空，默认 0 | 逻辑删除标记 |
| 索引 | — | idx_pp_patient_status、idx_pp_part_side、idx_pp_part | 支持按患者状态与部位侧别检索 |

表7　康复部位字典（rehab_part_dict / RehabPart）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| code | VARCHAR(30) | 主键（业务编码，不自增） | 康复部位编码 |
| name | VARCHAR(50) | 非空，唯一 | 康复部位名称 |
| group_code | VARCHAR(20) | 非空 | 部位大类 UPPER/LOWER/SPINE/OVERALL |
| sort_no | INT | 非空，默认 0 | 排序号 |
| enabled | TINYINT | 非空，默认 1 | 是否启用 |
| created_at / updated_at | DATETIME(3) | 非空 | 创建与更新时间（字典表不做逻辑删除） |

表8　标准动作（action / Action）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| action_id | BIGINT UNSIGNED | 主键，自增 | 动作 ID |
| name | VARCHAR(100) | 非空 | 动作名称 |
| target_part_code | VARCHAR(30) | 非空，外键→rehab_part_dict.code | 目标康复部位 |
| difficulty | VARCHAR(20) | 非空 | 难度 EASY/MEDIUM/HARD |
| description | TEXT | 可空 | 动作说明 |
| status | VARCHAR(20) | 非空 | DRAFT 草稿 / PUBLISHED 已发布 / DISABLED 已停用 |
| created_by | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 创建人 |
| created_at / updated_at | DATETIME(3) | 非空 | 创建与更新时间 |
| deleted | TINYINT | 非空，默认 0 | 逻辑删除标记 |
| 索引 | — | idx_action_part_status(target_part_code, status) | 按部位与状态检索动作 |
