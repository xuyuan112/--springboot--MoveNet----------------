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
