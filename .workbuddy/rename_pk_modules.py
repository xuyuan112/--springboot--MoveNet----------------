# -*- coding: utf-8 -*-
"""将 CDM 模块 SQL 文件的通用主键 id 重命名为 表前缀_id（仅重命名，不改结构）。"""
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

BASE = r'F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\ER优化SQL'
SQL_FILES = [
    BASE + r'\cdm_00_overview.sql',
    BASE + r'\cdm_02_action_algorithm.sql',
    BASE + r'\cdm_03_plan_schedule.sql',
    BASE + r'\cdm_04_training_scoring.sql',
]

CREATE_RE = re.compile(r'CREATE TABLE (\w+) \(\n(.*?)\n\)([^\n]*\n)', re.DOTALL)


def fix_sql(path):
    with open(path, encoding='utf-8') as f:
        s = f.read()

    def block_fix(m):
        t, body, suffix = m.group(1), m.group(2), m.group(3)
        if t not in MAPPING:
            return m.group(0)
        p = MAPPING[t]
        body = re.sub(r'(?m)^(\s*)id(\s+BIGINT)', rf'\g<1>{p}\g<2>', body, count=1)
        body = body.replace('PRIMARY KEY (id)', f'PRIMARY KEY ({p})')
        return f'CREATE TABLE {t} (\n{body}\n){suffix}'
    s = CREATE_RE.sub(block_fix, s)

    for t, p in MAPPING.items():
        s = s.replace(f'REFERENCES {t}(id,', f'REFERENCES {t}({p},')
        s = s.replace(f'REFERENCES {t} (id,', f'REFERENCES {t} ({p},')
        s = s.replace(f'REFERENCES {t}(id)', f'REFERENCES {t}({p})')
        s = s.replace(f'REFERENCES {t} (id)', f'REFERENCES {t} ({p})')

    s = s.replace('uk_binding_id_pair (id,', 'uk_binding_id_pair (binding_id,')
    s = s.replace('uk_rule_id_version (id,action_version_id)',
                  'uk_rule_id_version (action_rule_id,action_version_id)')
    s = s.replace('symmetry_pair_rule_id <> id', 'symmetry_pair_rule_id <> action_rule_id')

    s = re.sub(r'ALTER TABLE (\w+) MODIFY id\b',
               lambda m: f'ALTER TABLE {m.group(1)} MODIFY {MAPPING[m.group(1)]}', s)

    # 头部注释中的旧主键引用
    s = s.replace('training_record.daily_task_id   -> plan_daily_task.id',
                  'training_record.daily_task_id   -> plan_daily_task.task_id')
    s = s.replace('training_record.action_version_id -> action_version.id',
                  'training_record.action_version_id -> action_version.action_version_id')
    s = s.replace('training_error_log.record_id    -> training_record.id',
                  'training_error_log.record_id    -> training_record.record_id')
    s = s.replace('training_error_log.rule_id      -> action_rule.id',
                  'training_error_log.rule_id      -> action_rule.action_rule_id')
    s = s.replace('action_rule.action_version_id   -> action_version.id',
                  'action_rule.action_version_id   -> action_version.action_version_id')
    s = s.replace('assessment_report.rehab_plan_id -> rehab_plan.id',
                  'assessment_report.plan_id       -> rehab_plan.plan_id')
    s = s.replace('assessment_report.patient_id    -> user_account.id',
                  'assessment_report.reviewer_id   -> user_account.user_id')
    s = s.replace('assessment_report.doctor_user_id -> user_account.id',
                  '')
    s = s.replace('assessment_report.reviewer_id   -> user_account.id',
                  '')
    s = s.replace('rehab_plan.patient_id/therapist_id -> user_account.id',
                  'rehab_plan.patient_id/therapist_id -> user_account.user_id')
    s = s.replace('training_record.daily_task_id   -> plan_daily_task.id',
                  'training_record.daily_task_id   -> plan_daily_task.task_id')

    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(s)
    print(f'OK: {path}')


for p in SQL_FILES:
    fix_sql(p)
