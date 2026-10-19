# -*- coding: utf-8 -*-
"""生成 CDM-03 康复计划与任务调度 PowerDesigner 经典 E/R 风格预览图 (SVG)
修复：1) <pi>/<ai> 转义 2) 外部实体副标题与分隔线重叠 3) <ai> 标记与长类型名重叠
"""
import re

OUT = r"F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\ER图_CDM-03_康复计划与任务调度_PowerDesigner风格预览.svg"

FONT = "Microsoft YaHei, SimHei, sans-serif"
C_ENT = "#a5dddd"; C_ENT_ST = "#1f7a7a"; C_EXT = "#f2fbfb"
C_REL = "#e3d3f5"; C_REL_ST = "#6a3fa0"
C_PI = "#b30000"; C_AI = "#b36b00"; C_TXT = "#123"; C_SUB = "#5a7a7a"

def text_w(t, size=12):
    w = 0
    for ch in t:
        w += size if ord(ch) > 0x2E7F else size * 0.58
    return w

s = []
s.append('<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="1180" viewBox="0 0 1600 1180" font-family="%s">' % FONT)
s.append('<rect x="0" y="0" width="1600" height="1180" fill="#ffffff"/>')
s.append('<text x="800" y="38" text-anchor="middle" font-size="22" font-weight="bold" fill="#222">CDM-03 康复计划与任务调度（Plan &amp; Schedule）— PowerDesigner 风格 CDM 预览</text>')

# ---------- 连线 ----------
lines = [
    ("340,205 390,205", [(348,196,"1,1","start")]),
    ("560,218 600,445", [(566,420,"1,n","start")]),
    ("1400,356 1335,560", [(1405,402,"1,1","start")]),
    ("1335,604 1335,750 1000,750 1000,870 920,870", [(928,858,"1,n","start")]),
    ("760,746 760,760", [(845,755,"1,1","start")]),
    ("760,808 760,810", [(845,806,"1,n","start")]),
    ("600,662 490,664", [(588,652,"1,1","start")]),
    ("320,664 230,664 230,830", [(236,816,"1,n","start")]),
    ("920,980 980,908", [(930,970,"1,1","start")]),
    ("1150,908 1240,962", [(1172,922,"1,n","start")]),
]
for pts, _ in lines:
    s.append('<polyline points="%s" fill="none" stroke="%s" stroke-width="1.3"/>' % (pts, C_ENT_ST))
for _, labels in lines:
    for x, y, t, a in labels:
        s.append('<text x="%d" y="%d" font-size="12" font-style="italic" fill="#0b5c5c" text-anchor="%s">%s</text>' % (x, y, a, t))

# ---------- 实体 ----------
def esc(t):
    return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

def entity(x, y, w, title, sub, rows, idents, external=False):
    rh = 18
    h = 34 + (16 if sub else 0) + rh * len(rows) + rh * len(idents) + 8
    fill = C_EXT if external else C_ENT
    dash = ' stroke-dasharray="7 4"' if external else ""
    s.append('<rect x="%d" y="%d" width="%d" height="%d" fill="%s" stroke="%s" stroke-width="1.4"%s/>' % (x, y, w, h, fill, C_ENT_ST, dash))
    s.append('<text x="%d" y="%d" text-anchor="middle" font-size="15" font-weight="bold" fill="#063636">%s</text>' % (x + w // 2, y + 23, esc(title)))
    yy = y + 34
    if sub:
        s.append('<text x="%d" y="%d" text-anchor="middle" font-size="11" fill="%s">%s</text>' % (x + w // 2, yy + 3, C_SUB, esc(sub)))
        yy += 16
    s.append('<line x1="%d" y1="%d" x2="%d" y2="%d" stroke="%s" stroke-width="1"/>' % (x + 6, yy + 1, x + w - 6, yy + 1, C_ENT_ST))
    for cn_en, marker, typ in rows:
        yy += rh
        s.append('<text x="%d" y="%d" font-size="12" fill="%s">%s</text>' % (x + 10, yy - 5, C_TXT, esc(cn_en)))
        type_x = x + w - 10
        if marker:
            mc = C_PI if marker == "<pi>" else C_AI
            mk_end = type_x - text_w(typ) - 8
            s.append('<text x="%d" y="%d" font-size="12" text-anchor="end" fill="%s">%s </text>' % (mk_end, yy - 5, mc, esc(marker)))
        s.append('<text x="%d" y="%d" font-size="12" text-anchor="end" fill="#333">%s</text>' % (type_x, yy - 5, esc(typ)))
    for ident in idents:
        yy += rh
        s.append('<text x="%d" y="%d" font-size="11" fill="#444">%s</text>' % (x + 10, yy - 5, esc(ident)))
    return h

entity(40, 70, 300, "患者治疗师关系 (patient_therapist)", "外部引用实体 · 见 CDM-01", [
    ("绑定ID (binding_id)", "<pi>", "BIGINT"),
    ("患者ID (patient_id)", "<ai>", "BIGINT"),
    ("治疗师ID (therapist_id)", "<ai>", "BIGINT"),
    ("关系类型 (relation_type)", "", "VARCHAR(20)"),
    ("状态 (status)", "", "VARCHAR(20)"),
    ("生效时间 (start_at)", "", "DATETIME"),
    ("结束时间 (end_at)", "", "DATETIME"),
    ("创建人ID (created_by)", "", "BIGINT"),
    ("创建时间 (created_at)", "", "DATETIME"),
], ["Identifier_1 <pi>", "Identifier_2 <ai>"], external=True)

entity(1260, 70, 300, "动作版本 (action_version)", "外部引用实体 · 见 CDM-02", [
    ("动作版本ID (action_version_id)", "<pi>", "BIGINT"),
    ("标准动作ID (action_id)", "<ai>", "BIGINT"),
    ("版本号 (version_no)", "<ai>", "INT"),
    ("动作类型 (action_type)", "", "VARCHAR(20)"),
    ("算法参数快照 (algo_config)", "", "JSON"),
    ("动作签名 (motion_signature)", "", "JSON"),
    ("姿态引擎版本 (pose_engine_id)", "", "VARCHAR(150)"),
    ("状态 (status)", "", "VARCHAR(20)"),
    ("创建人ID (created_by)", "", "BIGINT"),
    ("创建时间 (created_at)", "", "DATETIME"),
    ("更新时间 (updated_at)", "", "DATETIME"),
    ("逻辑删除 (deleted)", "", "TINYINT"),
], ["Identifier_1 <pi>", "Identifier_2 <ai>"], external=True)

entity(600, 380, 320, "康复计划 (rehab_plan)", "", [
    ("计划ID (plan_id)", "<pi>", "BIGINT"),
    ("患者ID (patient_id)", "<ai>", "BIGINT"),
    ("治疗师ID (therapist_id)", "", "BIGINT"),
    ("绑定ID (binding_id)", "", "BIGINT"),
    ("计划名称 (name)", "<ai>", "VARCHAR(100)"),
    ("康复部位编码 (rehab_part_code)", "", "VARCHAR(30)"),
    ("版本号 (version_no)", "<ai>", "INT"),
    ("开始日期 (start_date)", "", "DATE"),
    ("结束日期 (end_date)", "", "DATE"),
    ("频率类型 (frequency_type)", "", "VARCHAR(20)"),
    ("频率值 (frequency_value)", "", "INT"),
    ("状态 (status)", "", "VARCHAR(20)"),
    ("备注 (remark)", "", "TEXT"),
    ("创建时间 (created_at)", "", "DATETIME"),
    ("更新时间 (updated_at)", "", "DATETIME"),
    ("逻辑删除 (deleted)", "", "TINYINT"),
], ["Identifier_1 <pi>", "Identifier_2 <ai>"])

entity(600, 810, 320, "计划动作项 (plan_item)", "", [
    ("动作项ID (plan_item_id)", "<pi>", "BIGINT"),
    ("计划ID (plan_id)", "<ai>", "BIGINT"),
    ("动作版本ID (action_version_id)", "", "BIGINT"),
    ("顺序号 (sequence_no)", "<ai>", "INT"),
    ("目标组数 (target_sets)", "", "INT"),
    ("目标次数 (target_reps)", "", "INT"),
    ("目标幅度 (target_amplitude)", "", "DECIMAL(6,2)"),
    ("持续时长秒 (duration_sec)", "", "INT"),
    ("休息时长秒 (rest_sec)", "", "INT"),
    ("动作指导 (instruction)", "", "TEXT"),
    ("创建时间 (created_at)", "", "DATETIME"),
    ("更新时间 (updated_at)", "", "DATETIME"),
    ("逻辑删除 (deleted)", "", "TINYINT"),
], ["Identifier_1 <pi>", "Identifier_2 <ai>"])

entity(80, 830, 300, "计划执行日程 (plan_schedule)", "", [
    ("日程ID (schedule_id)", "<pi>", "BIGINT"),
    ("计划ID (plan_id)", "<ai>", "BIGINT"),
    ("执行星期 (weekday)", "", "TINYINT"),
    ("创建时间 (created_at)", "", "DATETIME"),
], ["Identifier_1 <pi>", "Identifier_2 <ai>"])

entity(1240, 830, 320, "每日训练任务 (plan_daily_task)", "", [
    ("任务ID (task_id)", "<pi>", "BIGINT"),
    ("动作项ID (plan_item_id)", "<ai>", "BIGINT"),
    ("任务日期 (task_date)", "", "DATE"),
    ("目标组数 (target_sets)", "", "INT"),
    ("目标次数 (target_reps)", "", "INT"),
    ("状态 (status)", "", "VARCHAR(20)"),
    ("跳过原因 (skip_reason)", "", "VARCHAR(255)"),
    ("训练开始时间 (started_at)", "", "DATETIME"),
    ("完成时间 (completed_at)", "", "DATETIME"),
    ("创建时间 (created_at)", "", "DATETIME"),
    ("更新时间 (updated_at)", "", "DATETIME"),
    ("逻辑删除 (deleted)", "", "TINYINT"),
], ["Identifier_1 <pi>", "Identifier_2 <ai>"])

# ---------- 关系 ----------
def rel(x, y, w, title, rows):
    rh = 16
    h = 24 + rh * len(rows) + 8
    s.append('<rect x="%d" y="%d" width="%d" height="%d" rx="14" fill="%s" stroke="%s" stroke-width="1.3"/>' % (x, y, w, h, C_REL, C_REL_ST))
    s.append('<text x="%d" y="%d" text-anchor="middle" font-size="13" font-weight="bold" fill="#3d2266">%s</text>' % (x + w // 2, y + 17, esc(title)))
    yy = y + 24
    for cn_en in rows:
        yy += rh
        s.append('<text x="%d" y="%d" font-size="11" fill="#333">%s</text>' % (x + 12, yy - 4, esc(cn_en)))
    return h

rel(390, 182, 170, "制定 (Create)", ["绑定ID (binding_id)", "患者ID (patient_id)", "治疗师ID (therapist_id)"])
rel(1250, 560, 180, "选用 (Select)", ["动作版本ID (action_version_id)"])
rel(680, 760, 160, "包含 (Contain)", ["计划ID (plan_id)"])
rel(320, 644, 170, "排期 (Schedule)", ["计划ID (plan_id)"])
rel(980, 898, 170, "生成 (Generate)", ["动作项ID (plan_item_id)"])

# ---------- 图例 ----------
lx, ly, lw, lh = 40, 1010, 400, 108
s.append('<rect x="%d" y="%d" width="%d" height="%d" fill="#fafafa" stroke="#999"/>' % (lx, ly, lw, lh))
s.append('<text x="%d" y="%d" font-size="13" font-weight="bold" fill="#333">图例 Legend</text>' % (lx + 12, ly + 20))
s.append('<rect x="%d" y="%d" width="34" height="16" fill="%s" stroke="%s"/>' % (lx + 12, ly + 30, C_ENT, C_ENT_ST))
s.append('<text x="%d" y="%d" font-size="12" fill="#333">核心实体（本模块 CDM-03）</text>' % (lx + 54, ly + 43))
s.append('<rect x="%d" y="%d" width="34" height="16" fill="%s" stroke="%s" stroke-dasharray="7 4"/>' % (lx + 12, ly + 52, C_EXT, C_ENT_ST))
s.append('<text x="%d" y="%d" font-size="12" fill="#333">外部引用实体（见 CDM-01 / CDM-02）</text>' % (lx + 54, ly + 65))
s.append('<rect x="%d" y="%d" width="34" height="16" rx="8" fill="%s" stroke="%s"/>' % (lx + 12, ly + 74, C_REL, C_REL_ST))
s.append('<text x="%d" y="%d" font-size="12" fill="#333">关系（Relationship）及其属性</text>' % (lx + 54, ly + 87))
s.append('<text x="%d" y="%d" font-size="12" fill="#333">&lt;pi&gt; 主标识　&lt;ai&gt; 次标识（唯一性约束）　连线两端 1,1 / 1,n 为基数</text>' % (lx + 12, ly + 102))

s.append('<text x="800" y="1152" text-anchor="middle" font-size="13" fill="#666">跨模块引用（不建物理外键）：patient_id / therapist_id / created_by → user_account（CDM-01）；rehab_part_code → rehab_part_dict、action_id → action（CDM-02）</text>')

s.append('</svg>')

with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(s))
import xml.etree.ElementTree as ET
ET.parse(OUT)
print("written & XML valid:", OUT)
