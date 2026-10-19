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
