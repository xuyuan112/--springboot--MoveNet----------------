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

表9　动作版本（action_version / ActionVersion）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| action_version_id | BIGINT UNSIGNED | 主键，自增 | 动作版本 ID |
| action_id | BIGINT UNSIGNED | 非空，外键→action.action_id | 所属动作 |
| version_no | INT | 非空，组合唯一 uk_action_version(action_id, version_no) | 版本号 |
| action_type | VARCHAR(20) | 非空 | 动作类型 ROM 关节活动度 / STRENGTH 力量 / BALANCE 平衡 |
| algo_config | JSON | 非空 | 算法配置（采样率、平滑窗口、门限等） |
| motion_signature | JSON | 非空 | 运动特征签名（起始与峰值角度区间、时长区间等） |
| pose_engine_id | VARCHAR(150) | 非空 | 姿态引擎标识（模型摘要 + 运行时版本） |
| status | VARCHAR(20) | 非空 | DRAFT/PUBLISHED/DEPRECATED，发布后不可修改 |
| created_by | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 创建人 |
| created_at / updated_at | DATETIME(3) | 非空 | 创建与更新时间 |
| deleted | TINYINT | 非空，默认 0 | 逻辑删除标记 |
| 索引 | — | idx_action_version_status(action_id, status) | 查某动作的可用版本 |

表10　动作规则（action_rule / ActionRule）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| action_rule_id | BIGINT UNSIGNED | 主键，自增 | 规则 ID |
| action_version_id | BIGINT UNSIGNED | 非空，外键→action_version.action_version_id | 所属动作版本 |
| joint_name | VARCHAR(50) | 非空，组合唯一 uk_rule_dimension(action_version_id, joint_name, dimension) | 关节名称 |
| keypoint_indices | JSON | 非空 | 参与角度计算的关键点索引 |
| angle_formula | VARCHAR(100) | 非空 | 角度计算公式标识 |
| joint_role | VARCHAR(20) | 非空 | 关节角色 PRIMARY 主评估 / AUXILIARY 辅助 |
| dimension | VARCHAR(20) | 非空 | 评估维度 ANGLE/STABILITY/SYMMETRY/HOLD/SPEED |
| phase | VARCHAR(20) | 非空 | 动作阶段 START/CONCENTRIC/ECCENTRIC/HOLD/FULL |
| min_angle / max_angle / standard_angle | DECIMAL(6,2) | 可空 | 最小、最大与标准角度（度） |
| tolerance_soft / tolerance_hard | DECIMAL(6,2) | 可空 | 软容差（提示级）与硬容差（纠错级） |
| hold_frames_required | INT | 可空 | 保持所需帧数 |
| speed_min / speed_max | DECIMAL(6,2) | 可空 | 角速度允许范围（度/秒） |
| score_weight | DECIMAL(5,4) | 非空，CHECK 取值 0~1 | 该规则在评分中的权重 |
| severity | TINYINT | 非空，CHECK 不小于 0 | 严重等级（越大越严重） |
| prompt_text | VARCHAR(255) | 非空 | 纠错提示文案 |
| symmetry_pair_rule_id | BIGINT UNSIGNED | 可空，复合外键 (symmetry_pair_rule_id, action_version_id)→(action_rule_id, action_version_id) | 对称评估配对规则，保证与新规则同版本 |
| 组合唯一键 | — | uk_rule_id_version(action_rule_id, action_version_id) | 供自身复合外键引用 |
| created_at / updated_at / deleted | DATETIME(3) / TINYINT | 非空 | 审计字段 |
| 表级约束 | — | 触发器 trg_action_rule_bi、trg_action_rule_bu | 禁止配对规则指向自身（MySQL 不允许 CHECK 引用自增列） |

表11　标准动作模板（action_template / ActionTemplate）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| template_id | BIGINT UNSIGNED | 主键，自增 | 模板 ID |
| action_version_id | BIGINT UNSIGNED | 非空，外键→action_version.action_version_id | 所属动作版本 |
| source_type | VARCHAR(20) | 非空，组合唯一 uk_template_source(action_version_id, source_type) | 模板来源 THERAPIST 治疗师录制 / ALGO 算法生成 |
| frame_rate | DECIMAL(6,2) | 非空 | 采样帧率 |
| frame_count | INT | 非空 | 帧数 |
| feature_dim | INT | 非空 | 特征维度 |
| feature_sequence | JSON | 非空 | 标准特征序列（DTW 比对基准） |
| keyframe_index | JSON | 可空 | 起始、峰值、保持关键帧索引 |
| sample_count | INT | 非空 | 采样样本数 |
| status | VARCHAR(20) | 非空 | ACTIVE / DEPRECATED |
| created_by | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 创建人 |
| created_at | DATETIME(3) | 非空 | 创建时间 |

表12　模板采样记录（action_template_sample / ActionTemplateSample）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| template_sample_id | BIGINT UNSIGNED | 主键，自增 | 采样记录 ID |
| template_id | BIGINT UNSIGNED | 非空，外键→action_template.template_id | 所属模板 |
| file_id | BIGINT UNSIGNED | 非空，外键→file_object.file_id | 示范视频文件 |
| sample_no | INT | 非空，组合唯一 uk_template_sample(template_id, sample_no) | 样本序号 |
| feature_hash | CHAR(64) | 非空 | 特征摘要（去重用） |
| captured_at | DATETIME(3) | 非空 | 采集时间 |
| created_by | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 采集人 |

表13　康复计划（rehab_plan / RehabPlan）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| plan_id | BIGINT UNSIGNED | 主键，自增 | 计划 ID |
| patient_id | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 患者用户 ID |
| therapist_id | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 制定治疗师用户 ID |
| binding_id | BIGINT UNSIGNED | 非空，复合外键 (binding_id, patient_id, therapist_id)→patient_therapist | 绑定关系，保证计划挂在有效绑定上 |
| name | VARCHAR(100) | 非空，组合唯一 uk_plan_version(patient_id, name, version_no) | 计划名称 |
| rehab_part_code | VARCHAR(30) | 非空，外键→rehab_part_dict.code | 康复部位 |
| version_no | INT | 非空，CHECK 大于 0 | 计划版本号（调整计划时递增） |
| start_date / end_date | DATE | 非空，CHECK end_date 不早于 start_date | 计划执行周期 |
| frequency_type | VARCHAR(20) | 非空 | 频次类型 DAILY 每日 / WEEKLY 每周 / CUSTOM 自定义 |
| frequency_value | INT | 可空 | 频次值（如每周 N 次） |
| status | VARCHAR(20) | 非空 | ACTIVE/COMPLETED/SUSPENDED/CANCELLED |
| remark | TEXT | 可空 | 备注 |
| created_at / updated_at / deleted | DATETIME(3) / TINYINT | 非空 | 审计字段 |
| 索引 | — | idx_plan_patient_status、idx_plan_binding | 按患者状态与绑定检索计划 |

表14　计划动作项（plan_item / PlanItem）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| plan_item_id | BIGINT UNSIGNED | 主键，自增 | 计划项 ID |
| plan_id | BIGINT UNSIGNED | 非空，外键→rehab_plan.plan_id | 所属计划 |
| action_version_id | BIGINT UNSIGNED | 非空，外键→action_version.action_version_id | 引用的动作版本 |
| sequence_no | INT | 非空，CHECK 大于 0，组合唯一 uk_plan_item_sequence(plan_id, sequence_no) | 动作顺序号 |
| target_sets | INT | 非空，CHECK 大于 0 | 目标组数 |
| target_reps | INT | 可空，CHECK 不小于 0 | 目标次数（每组） |
| target_amplitude | DECIMAL(6,2) | 可空 | 目标幅度（度） |
| duration_sec | INT | 可空，CHECK 不小于 0 | 单组时长（秒） |
| rest_sec | INT | 可空，CHECK 不小于 0 | 组间休息（秒） |
| instruction | TEXT | 可空 | 动作指导语 |
| created_at / updated_at / deleted | DATETIME(3) / TINYINT | 非空 | 审计字段 |

表15　计划执行日程（plan_schedule / PlanSchedule）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| schedule_id | BIGINT UNSIGNED | 主键，自增 | 日程 ID |
| plan_id | BIGINT UNSIGNED | 非空，外键→rehab_plan.plan_id | 所属计划 |
| weekday | TINYINT | 非空，CHECK 取值 1~7，组合唯一 uk_schedule_day(plan_id, weekday) | 星期几（1 周一 … 7 周日） |
| created_at | DATETIME(3) | 非空 | 创建时间 |

表16　每日训练任务（plan_daily_task / PlanDailyTask）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| task_id | BIGINT UNSIGNED | 主键，自增 | 任务 ID |
| plan_item_id | BIGINT UNSIGNED | 非空，外键→plan_item.plan_item_id | 来源计划动作项 |
| task_date | DATE | 非空，组合唯一 uk_task_item_date(plan_item_id, task_date) | 任务日期 |
| target_sets | INT | 非空，CHECK 大于 0 | 目标组数（生成任务时快照） |
| target_reps | INT | 可空，CHECK 不小于 0 | 目标次数（生成任务时快照） |
| status | VARCHAR(20) | 非空 | PENDING/IN_PROGRESS/COMPLETED/SKIPPED/EXPIRED |
| skip_reason | VARCHAR(255) | 可空 | 跳过原因 |
| started_at / completed_at | DATETIME(3) | 可空 | 开始时间与完成时间 |
| created_at / updated_at / deleted | DATETIME(3) / TINYINT | 非空 | 审计字段 |
| 索引 | — | idx_task_date_status(task_date, status) | 按日期与状态检索当日任务 |

表17　训练记录（training_record / TrainingRecord）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| record_id | BIGINT UNSIGNED | 主键，自增 | 训练记录 ID |
| daily_task_id | BIGINT UNSIGNED | 非空，外键→plan_daily_task.task_id | 来源每日任务 |
| action_version_id | BIGINT UNSIGNED | 非空，外键→action_version.action_version_id | 动作版本快照（提交时复制自计划项） |
| started_at / ended_at | DATETIME(3) | 非空，CHECK ended_at 不早于 started_at | 训练起止时间 |
| total_reps | INT | 非空，CHECK 不小于 0 | 总次数 |
| correct_reps | INT | 非空，CHECK 介于 0 与 total_reps 之间 | 标准（达标）次数 |
| total_score | DECIMAL(5,2) | 非空，CHECK 取值 0~100 | 综合得分 |
| score_detail | JSON | 非空 | 六维评分快照（幅度/保持/节奏/稳定/对称/相似度） |
| avg_rom | DECIMAL(6,2) | 可空 | 平均关节活动度（度） |
| peak_angle / trough_angle | DECIMAL(6,2) | 可空 | 峰值角度与谷值角度（度） |
| peak_frame_no / trough_frame_no | INT | 可空 | 峰值与谷值所在帧号 |
| hold_ratio | DECIMAL(5,4) | 可空 | 保持达标率（0~1） |
| template_similarity | DECIMAL(5,4) | 可空 | 与标准模板的相似度（0~1） |
| valid_frame_ratio | DECIMAL(5,4) | 可空 | 有效帧占比（0~1） |
| pose_repaired_ratio | DECIMAL(5,4) | 可空 | 关键点修复帧占比（0~1） |
| stability_score / symmetry_score | DECIMAL(5,2) | 可空 | 稳定性得分与对称性得分（0~100） |
| pose_quality | VARCHAR(20) | 非空 | 姿态质量 GOOD/FAIR/POOR |
| pose_engine_id | VARCHAR(150) | 非空 | 姿态引擎标识（分析时快照） |
| client_request_id | VARCHAR(100) | 非空，组合唯一 uk_record_idempotency(daily_task_id, client_request_id) | 客户端请求 ID（幂等防重） |
| remark | TEXT | 可空 | 备注 |
| created_at | DATETIME(3) | 非空 | 写入时间 |
| 索引 | — | idx_record_task_time(daily_task_id,created_at)、idx_record_version_time(action_version_id,created_at) | 支持按任务与按动作版本聚合趋势 |

表18　训练纠错日志（training_error_log / TrainingErrorLog）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| error_id | BIGINT UNSIGNED | 主键，自增 | 纠错日志 ID |
| record_id | BIGINT UNSIGNED | 非空，外键→training_record.record_id | 所属训练记录 |
| rule_id | BIGINT UNSIGNED | 可空，外键→action_rule.action_rule_id | 触发规则（非规则类错误为 NULL） |
| dimension | VARCHAR(20) | 非空 | 评估维度 |
| phase | VARCHAR(20) | 非空 | 动作阶段 |
| error_type | VARCHAR(40) | 非空 | 错误类型（如 AMPLITUDE 幅度不足、SPEED 过快） |
| joint_name | VARCHAR(50) | 可空 | 关节名称 |
| current_value / expected_value / deviation | DECIMAL(8,3) | 可空 | 实测值、期望值与偏差 |
| severity | TINYINT | 非空 | 严重等级 |
| frame_no | INT | 可空 | 发生帧号（连续区间起始帧） |
| prompt_text | VARCHAR(255) | 非空 | 提示文案快照 |
| error_at | DATETIME(3) | 非空 | 错误发生时间 |
| created_at | DATETIME(3) | 非空 | 写入时间 |
| 索引 | — | idx_error_record(record_id)、idx_error_dimension(dimension, severity) | 按记录与维度严重度检索 |

表19　评估报告（assessment_report / AssessmentReport）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| report_id | BIGINT UNSIGNED | 主键，自增 | 报告 ID |
| plan_id | BIGINT UNSIGNED | 非空，外键→rehab_plan.plan_id | 来源计划 |
| period_start / period_end | DATE | 非空，CHECK period_end 不早于 period_start，组合唯一 uk_report_period(plan_id, period_start, period_end) | 统计周期 |
| data_snapshot | JSON | 非空 | 报告数据快照（指标聚合结果） |
| ai_draft | TEXT | 可空 | AI 生成的报告草稿（不可直接发布） |
| therapist_conclusion | TEXT | 可空 | 治疗师审核结论 |
| status | VARCHAR(20) | 非空 | DRAFT/AI_GENERATED/REVIEWED/PUBLISHED |
| reviewed_by | BIGINT UNSIGNED | 可空，外键→user_account.user_id | 审核治疗师 |
| reviewed_at | DATETIME(3) | 可空 | 审核时间 |
| created_at / updated_at | DATETIME(3) | 非空 | 创建与更新时间 |

表20　公共内容（content / Content）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| content_id | BIGINT UNSIGNED | 主键，自增 | 内容 ID |
| title | VARCHAR(200) | 非空 | 标题 |
| content_type | VARCHAR(30) | 非空 | 内容类型 ARTICLE 文章 / VIDEO 视频 / GUIDE 指南 |
| target_part_code | VARCHAR(30) | 非空，外键→rehab_part_dict.code | 关联康复部位 |
| target_group | VARCHAR(100) | 可空 | 目标人群标签 |
| summary | VARCHAR(500) | 可空 | 摘要 |
| content_body | LONGTEXT | 非空 | 正文 |
| author_id | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 作者 |
| status | VARCHAR(20) | 非空 | DRAFT/PENDING_REVIEW/PUBLISHED/OFFLINE |
| reviewer_id | BIGINT UNSIGNED | 可空，外键→user_account.user_id | 审核人 |
| review_remark | VARCHAR(500) | 可空 | 审核意见 |
| valid_from | DATETIME(3) | 非空 | 生效时间 |
| valid_to | DATETIME(3) | 可空，CHECK 不早于 valid_from | 失效时间（长期有效为 NULL） |
| published_at | DATETIME(3) | 可空 | 发布时间 |
| created_at / updated_at / deleted | DATETIME(3) / TINYINT | 非空 | 审计字段 |
| 索引 | — | idx_content_filter(status, target_part_code, valid_from, valid_to) | 患者端按状态、部位、有效期过滤 |

表21　内容附件（content_attachment / ContentAttachment）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| content_attachment_id | BIGINT UNSIGNED | 主键，自增 | 附件关联 ID |
| content_id | BIGINT UNSIGNED | 非空，外键→content.content_id | 所属内容 |
| file_id | BIGINT UNSIGNED | 非空，外键→file_object.file_id，组合唯一 uk_content_file(content_id, file_id) | MinIO 文件 |
| attachment_type | VARCHAR(20) | 非空 | 附件类型 COVER 封面 / VIDEO 视频 / IMAGE 图片 / DOC 文档 |
| sort_no | INT | 非空，默认 0 | 展示顺序 |
| created_at | DATETIME(3) | 非空 | 创建时间 |

表22　内容收藏（content_collection / ContentCollection）

| 字段名 | 数据类型 | 约束条件 | 说明 |
| --- | --- | --- | --- |
| collection_id | BIGINT UNSIGNED | 主键，自增 | 收藏 ID |
| user_id | BIGINT UNSIGNED | 非空，外键→user_account.user_id | 收藏用户 |
| content_id | BIGINT UNSIGNED | 非空，外键→content.content_id，组合唯一 uk_collection_user_content(user_id, content_id) | 被收藏内容 |
| collected_at | DATETIME(3) | 非空 | 收藏时间 |

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
