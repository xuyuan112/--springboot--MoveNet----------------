# -*- coding: utf-8 -*-
"""统一将所有表的通用主键 id 重命名为 表前缀_id，并同步所有 REFERENCES/主键声明。"""
import re

MAPPING = {
    'user_account': 'user_id',
    'patient_profile': 'patient_profile_id',
    'patient_rehab_part': 'patient_rehab_part_id',
    'therapist_profile': 'therapist_profile_id',
    'patient_therapist': 'binding_id',
    'action': 'action_id',
    'action_version': 'action_version_id',
    'action_rule': 'action_rule_id',
    'file_object': 'file_id',
    'action_template': 'template_id',
    'action_template_sample': 'template_sample_id',
    'rehab_plan': 'plan_id',
    'plan_item': 'plan_item_id',
    'plan_schedule': 'schedule_id',
    'plan_daily_task': 'task_id',
    'training_record': 'record_id',
    'training_error_log': 'error_id',
    'assessment_report': 'report_id',
    'content': 'content_id',
    'content_attachment': 'content_attachment_id',
    'content_collection': 'collection_id',
    'consultation': 'consultation_id',
    'consultation_message': 'message_id',
    'consultation_attachment': 'consultation_attachment_id',
    'operation_log': 'log_id',
    'announcement': 'announcement_id',
}

SQL_FILES = [
    r'F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\rehab_platform_pd.sql',
    r'F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\rehab_platform.sql',
    r'F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\ER优化SQL\cdm_01_account_binding.sql',
]

CREATE_RE = re.compile(r'CREATE TABLE (\w+) \(\n(.*?)\n\)([^\n]*\n)', re.DOTALL)


def fix_sql(path):
    with open(path, encoding='utf-8') as f:
        s = f.read()

    # 1) CREATE TABLE 块内的主键列名与 PRIMARY KEY (id)
    def block_fix(m):
        t, body, suffix = m.group(1), m.group(2), m.group(3)
        if t not in MAPPING:
            return m.group(0)
        p = MAPPING[t]
        body = re.sub(r'(?m)^(\s*)id(\s+BIGINT)', rf'\g<1>{p}\g<2>', body, count=1)
        body = body.replace('PRIMARY KEY (id)', f'PRIMARY KEY ({p})')
        return f'CREATE TABLE {t} (\n{body}\n){suffix}'
    s = CREATE_RE.sub(block_fix, s)

    # 2) 所有 REFERENCES 指向的主键列
    for t, p in MAPPING.items():
        s = s.replace(f'REFERENCES {t}(id,', f'REFERENCES {t}({p},')
        s = s.replace(f'REFERENCES {t} (id,', f'REFERENCES {t} ({p},')
        s = s.replace(f'REFERENCES {t}(id)', f'REFERENCES {t}({p})')
        s = s.replace(f'REFERENCES {t} (id)', f'REFERENCES {t} ({p})')

    # 3) 复合唯一键 / CHECK 中的 id
    s = s.replace('uk_binding_id_pair (id,', 'uk_binding_id_pair (binding_id,')
    s = s.replace('uk_binding_id_pair (id, patient_id, therapist_id)',
                  'uk_binding_id_pair (binding_id, patient_id, therapist_id)')
    s = s.replace('uk_rule_id_version (id,action_version_id)',
                  'uk_rule_id_version (action_rule_id,action_version_id)')
    s = s.replace('symmetry_pair_rule_id <> id', 'symmetry_pair_rule_id <> action_rule_id')

    # 4) AUTO_INCREMENT 的 ALTER 语句
    s = re.sub(r'ALTER TABLE (\w+) MODIFY id\b',
               lambda m: f'ALTER TABLE {m.group(1)} MODIFY {MAPPING[m.group(1)]}', s)

    # 5) 种子数据 INSERT 的列名
    s = s.replace('INSERT IGNORE INTO user_account\n (id,', 'INSERT IGNORE INTO user_account\n (user_id,')

    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(s)
    print(f'OK: {path}')


for p in SQL_FILES:
    fix_sql(p)

# ---------- 数据库设计.md ----------
MD = r'F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\数据库设计.md'
with open(MD, encoding='utf-8') as f:
    md = f.read()

# 第3节公共字段说明：主键命名约定
md = md.replace(
    '`id BIGINT UNSIGNED PK`；',
    '主键统一命名为 `{表前缀}_id BIGINT UNSIGNED PK`（如 `user_account.user_id`、`rehab_plan.plan_id`、`patient_therapist.binding_id`，见各表节）；')

# 按节处理：节标题中含 `表名` 时，替换节内第一个 `id PK` 和第一个 "| id | ... PK |" 行
HEADER_RE = re.compile(r'(?m)^(#{2,4} .+`(\w+)`[^`\n]*)$')
parts = HEADER_RE.split(md)
# re.split 带两个分组：输出为 [前缀, 标题行, 表名, 正文, 标题行, 表名, 正文, ...]
out = [parts[0]]
i = 1
while i < len(parts):
    header, table, body = parts[i], parts[i + 1], parts[i + 2]
    if table in MAPPING:
        p = MAPPING[table]
        body = re.sub(r'`id PK`', f'`{p} PK`', body, count=1)
        body = re.sub(r'(?m)^\| id \| BIGINT UNSIGNED \| PK \|',
                      f'| {p} | BIGINT UNSIGNED | PK |', body, count=1)
    out.append(header)
    out.append(body)
    i += 3
md = ''.join(out)

# FK 目标表.主键 的写法同步
md = md.replace('user_account.id', 'user_account.user_id')
md = md.replace('action_version.id', 'action_version.action_version_id')
md = md.replace('action_rule.id', 'action_rule.action_rule_id')
md = md.replace('file_object.id', 'file_object.file_id')

# 追加修订说明
md += '''
# 主键命名统一（2026-09-16）

为解决所有表主键同名 `id` 带来的歧义，本次将全部业务表主键改为「表前缀_id」命名：

- 规则：`<表名前缀>_id`，如 `user_account.user_id`、`rehab_plan.plan_id`、`plan_daily_task.task_id`、`training_record.record_id`；
- 特例：`patient_therapist` 主键命名为 `binding_id`（与 `rehab_plan.binding_id`、`consultation.binding_id` 外键列名一致，语义即"绑定ID"）；`rehab_part_dict` 仍以业务编码 `code` 为主键，不变；
- 外键**列名保持不变**（如 `created_by`、`daily_task_id`、`rule_id`、`symmetry_pair_rule_id`），仅 `REFERENCES` 指向的目标列名随主键更新；
- 因多数外键列名本就与新主键同名（`action_id`、`action_version_id`、`plan_id`、`plan_item_id`、`template_id`、`file_id`、`content_id`、`binding_id`、`record_id`、`message_id`、`consultation_id`），关联查询更直观；
- `rehab_platform.sql`（UNSIGNED/DATETIME(3)）、`rehab_platform_pd.sql`（PowerDesigner 兼容版）与 `ER优化SQL/cdm_01_account_binding.sql` 已同步修改；接口层出参仍可映射为 `id`/`userId` 等 DTO 字段，不受存储层命名影响。
'''
with open(MD, 'w', encoding='utf-8', newline='') as f:
    f.write(md)
print(f'OK: {MD}')
