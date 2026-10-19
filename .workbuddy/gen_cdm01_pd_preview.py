# -*- coding: utf-8 -*-
"""CDM-01 预览图：PowerDesigner CDM 风格（实体框 + IE 联系线，区分标识/非标识关系）"""
import html

W, H = 2200, 1420
S = []

def esc(s):
    return html.escape(s, quote=False)

def txt(x, y, s, size=13, fill="#333", anchor="start", bold=False, halo=True):
    fw = ' font-weight="600"' if bold else ''
    pb = ' paint-order="stroke" stroke="#FFFFFF" stroke-width="3" stroke-linejoin="round"' if halo else ''
    S.append(f'<text x="{x}" y="{y}" font-size="{size}" fill="{fill}" text-anchor="{anchor}"{fw}{pb} font-family="Microsoft YaHei, PingFang SC, sans-serif">{esc(s)}</text>')

def entity(x, y, w, cn, code, rows):
    """rows: [(中文名, 列名, 是否主标识P, 是否非空M)]"""
    n = len(rows)
    h = 34 + 22 * n + 10
    S.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="#FFFFFF" stroke="#4F4F4F" stroke-width="1.1"/>')
    S.append(f'<rect x="{x}" y="{y}" width="{w}" height="34" fill="#DCE6F1" stroke="#4F4F4F" stroke-width="1.1"/>')
    txt(x + w / 2, y + 16, cn, size=14.5, fill="#1F3864", anchor="middle", bold=True, halo=False)
    txt(x + w / 2, y + 30, code, size=10.5, fill="#666666", anchor="middle", halo=False)
    ry = y + 49
    for cn2, en, p, m in rows:
        txt(x + 12, ry, cn2, size=12.5, fill="#222222", halo=False)
        txt(x + 152, ry, en, size=11, fill="#888888", halo=False)
        tags = ""
        if p:
            tags += '<tspan fill="#C00000" font-weight="600">（P）</tspan>'
        if m:
            tags += '<tspan fill="#0070C0">（M）</tspan>'
        if tags:
            S.append(f'<text x="{x + w - 12}" y="{ry}" font-size="11.5" text-anchor="end" font-family="Microsoft YaHei, sans-serif">{tags}</text>')
        ry += 22
    return (x, y, w, h)

def poly(pts, identifying=False, label=None, lx=0, ly=0):
    dash = '' if identifying else ' stroke-dasharray="8,6"'
    color = '#333333' if identifying else '#555555'
    sw = 1.5 if identifying else 1.2
    pstr = " ".join(f"{a},{b}" for a, b in pts)
    S.append(f'<polyline points="{pstr}" fill="none" stroke="{color}" stroke-width="{sw}"{dash}/>')
    if label:
        txt(lx, ly, label, size=12.5, fill="#444444")

def card(x, y, s):
    txt(x, y, s, size=13, fill="#111111", bold=True)

# ---------- 标题 ----------
txt(W / 2, 42, "CDM-01 账户、患者与治疗师关系 —— PowerDesigner CDM 预览（IE 乌鸦脚表示法）", size=21, fill="#111111", anchor="middle", bold=True, halo=False)
txt(W / 2, 66, "实体框 = PD CDM 实体符号；（P）= 主标识，（M）= 非空；实线 = 标识关系，虚线 = 非标识关系；线端 1 / n = 基数", size=12, fill="#777777", anchor="middle", halo=False)

# ---------- 六个实体 ----------
UA = entity(850, 80, 300, "用户账号", "user_account", [
    ("用户ID", "user_id", True, True),
    ("登录名", "username", False, True),
    ("密码密文", "password_hash", False, True),
    ("手机号", "phone", False, False),
    ("邮箱", "email", False, False),
    ("角色编码", "role_code", False, True),
    ("状态", "status", False, True),
    ("最后登录时间", "last_login_at", False, False),
    ("创建时间", "created_at", False, True),
    ("更新时间", "updated_at", False, True),
    ("删除标记", "deleted", False, True),
])

PP = entity(90, 560, 300, "患者档案", "patient_profile", [
    ("用户ID", "user_id", True, True),
    ("真实姓名", "real_name", False, False),
    ("性别", "gender", False, False),
    ("出生日期", "birth_date", False, False),
    ("身高（cm）", "height_cm", False, False),
    ("体重（kg）", "weight_kg", False, False),
    ("活动水平", "activity_level", False, False),
    ("禁忌说明", "contraindication", False, False),
    ("创建时间", "created_at", False, True),
    ("更新时间", "updated_at", False, True),
    ("删除标记", "deleted", False, True),
])

TP = entity(1810, 560, 300, "治疗师档案", "therapist_profile", [
    ("用户ID", "user_id", True, True),
    ("姓名", "real_name", False, False),
    ("职称", "title", False, False),
    ("专业特长", "specialty", False, False),
    ("从业年限", "years_of_experience", False, False),
    ("执业资质编号", "license_no", False, False),
    ("个人简介", "bio", False, False),
    ("创建时间", "created_at", False, True),
    ("更新时间", "updated_at", False, True),
    ("删除标记", "deleted", False, True),
])

PT = entity(850, 660, 300, "患者-治疗师关系", "patient_therapist", [
    ("绑定ID", "binding_id", True, True),
    ("患者用户ID", "patient_id", False, True),
    ("治疗师用户ID", "therapist_id", False, True),
    ("关系类型", "relation_type", False, True),
    ("生效时间", "start_at", False, True),
    ("结束时间", "end_at", False, False),
    ("状态", "status", False, True),
    ("创建人", "created_by", False, True),
    ("创建时间", "created_at", False, True),
    ("删除标记", "deleted", False, True),
])

PRP = entity(850, 1090, 300, "患者康复部位", "patient_rehab_part", [
    ("患者康复部位ID", "patient_rehab_part_id", True, True),
    ("患者用户ID", "patient_id", False, True),
    ("部位编码", "rehab_part_code", False, True),
    ("侧别", "side", False, True),
    ("主部位标记", "primary_flag", False, False),
    ("确诊日期", "diagnosed_at", False, False),
    ("创建时间", "created_at", False, True),
    ("更新时间", "updated_at", False, True),
    ("删除标记", "deleted", False, True),
])

RPD = entity(1520, 1120, 300, "康复部位字典", "rehab_part_dict", [
    ("部位编码", "code", True, True),
    ("部位名称", "name", False, True),
    ("部位大类", "group_code", False, True),
    ("排序号", "sort_no", False, False),
    ("启用标记", "enabled", False, True),
    ("说明", "remark", False, False),
])

# ---------- 联系线 ----------
# 1) 用户账号 —(1:1 标识)— 患者档案：user_id 迁入为主标识
poly([(940, 366), (940, 560)], identifying=True, label="拥有", lx=954, ly=468)
card(948, 396, "1"); card(948, 548, "1")

# 2) 用户账号 —(1:1 标识)— 治疗师档案
poly([(1150, 160), (1960, 160), (1960, 560)], identifying=True, label="拥有", lx=1540, ly=148)
card(1158, 178, "1"); card(1948, 545, "1")

# 3) 用户账号 —(1:n 非标识)×3 — 患者-治疗师关系（患者/治疗师/创建人）
poly([(980, 366), (950, 660)], label="患者", lx=916, ly=500)
card(986, 390, "1"); card(956, 646, "n")
poly([(1030, 366), (1030, 660)], label="治疗师", lx=1040, ly=500)
card(1040, 390, "1"); card(1038, 646, "n")
poly([(1080, 366), (1110, 660)], label="创建人", lx=1120, ly=500)
card(1090, 390, "1"); card(1118, 646, "n")

# 4) 患者档案 —(1:n)— 患者-治疗师关系
poly([(390, 698), (630, 698), (630, 745), (850, 745)], label="负责绑定", lx=478, ly=688)
card(398, 690, "1"); card(838, 737, "n")

# 5) 治疗师档案 —(1:n)— 患者-治疗师关系
poly([(1810, 687), (1520, 687), (1520, 745), (1150, 745)], label="负责绑定", lx=1596, ly=677)
card(1802, 677, "1"); card(1162, 737, "n")

# 6) 患者档案 —(1:n)— 患者康复部位
poly([(200, 846), (200, 1190), (850, 1190)], label="涉及", lx=486, ly=1180)
card(208, 866, "1"); card(838, 1181, "n")

# 7) 康复部位字典 —(1:n)— 患者康复部位
poly([(1520, 1205), (1150, 1205)], label="属于", lx=1335, ly=1195)
card(1510, 1195, "1"); card(1162, 1195, "n")

# ---------- 注释 ----------
txt(1000, 950, "注：patient_id / therapist_id / created_by 由 3 条指向用户账号的 n:1 联系迁入（非标识，不入主标识）", size=11.5, fill="#777777", anchor="middle")
txt(1000, 1362, "注：rehab_part_code → rehab_part_dict.code；字典表属共享基础数据域，本图内画为正常实体", size=11.5, fill="#777777", anchor="middle")

# ---------- 图例 ----------
lx, ly, lw, lh = 1180, 190, 640, 168
S.append(f'<rect x="{lx}" y="{ly}" width="{lw}" height="{lh}" fill="#FAFAFA" stroke="#BBBBBB" stroke-width="0.8" rx="6"/>')
txt(lx + 14, ly + 24, "图例 / 与 Chen 图对照", size=13, fill="#111111", bold=True, halo=False)
txt(lx + 14, ly + 50, "（P）主标识 = Primary Identifier（生成 PDM 后即为 <pk>", size=12, fill="#444444", halo=False)
txt(lx + 14, ly + 74, "（M）非空 = Mandatory；未标（M）的列在 PDM 中可空", size=12, fill="#444444", halo=False)
txt(lx + 14, ly + 98, "实线 = 标识关系（identifying）：父表主标识迁入子表主标识", size=12, fill="#444444", halo=False)
txt(lx + 14, ly + 122, "虚线 = 非标识关系（non-identifying）：仅作普通外键引用", size=12, fill="#444444", halo=False)
txt(lx + 14, ly + 146, "患者-治疗师关系、患者康复部位 = 关联实体（n:m 解析为两条 1:n）", size=12, fill="#444444", halo=False)

svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}" font-family="Microsoft YaHei, PingFang SC, sans-serif">\n' + "\n".join(S) + "\n</svg>\n"
out = r"F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\ER图_CDM-01_PowerDesigner风格预览.svg"
open(out, "w", encoding="utf-8").write(svg)
print("written", out)
