# -*- coding: utf-8 -*-
"""CDM-01 预览图 v2：PowerDesigner CDM 经典 E/R（Merise）表示法
青色 = 实体；紫色圆角框 = 联系（含属性 = 实体化联系，同"选课/课程评价"）"""
import html

W, H = 2200, 1500
S = []

def esc(s):
    return html.escape(s, quote=False)

def txt(x, y, s, size=13, fill="#333", anchor="start", bold=False, halo=True):
    fw = ' font-weight="600"' if bold else ''
    pb = ' paint-order="stroke" stroke="#FFFFFF" stroke-width="3" stroke-linejoin="round"' if halo else ''
    S.append(f'<text x="{x}" y="{y}" font-size="{size}" fill="{fill}" text-anchor="{anchor}"{fw}{pb} font-family="Microsoft YaHei, PingFang SC, sans-serif">{esc(s)}</text>')

def line(pts, identifying=False):
    dash = '' if identifying else ' stroke-dasharray="8,6"'
    color = '#333333' if identifying else '#555555'
    sw = 1.6 if identifying else 1.2
    pstr = " ".join(f"{a},{b}" for a, b in pts)
    S.append(f'<polyline points="{pstr}" fill="none" stroke="{color}" stroke-width="{sw}"{dash}/>')

def card(x, y, s):
    txt(x, y, s, size=12.5, fill="#111111", bold=True)

def entity(x, y, w, cn, code, rows):
    """rows: [(中文名, 类型, pi, m)]"""
    n = len(rows)
    h = 26 + n * 20 + 20 + 8
    S.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="#CDF5F5" stroke="#0B7285" stroke-width="1.1"/>')
    txt(x + w / 2, y + 17, f"{cn}（{code}）", size=13.5, fill="#0B4F5C", anchor="middle", bold=True, halo=False)
    S.append(f'<line x1="{x}" y1="{y+26}" x2="{x+w}" y2="{y+26}" stroke="#0B7285" stroke-width="0.6"/>')
    ry = y + 42
    for cn2, tp, pi, m in rows:
        tags = '<tspan font-weight="600">&lt;pi&gt;</tspan> ' if pi else ''
        tail = ' <tspan fill="#0B7285">&lt;M&gt;</tspan>' if m else ''
        S.append(f'<text x="{x+12}" y="{ry}" font-size="12" fill="#222222" font-family="Consolas, Microsoft YaHei, sans-serif">{esc(cn2)} {tags}{esc(tp)}{tail}</text>')
        ry += 20
    S.append(f'<line x1="{x+10}" y1="{y+h-24}" x2="{x+w-10}" y2="{y+h-24}" stroke="#0B7285" stroke-width="0.5" stroke-dasharray="2,2"/>')
    txt(x + 14, y + h - 9, 'Identifier_1', size=11, fill="#666666", halo=False)
    S.append(f'<text x="{x+w-14}" y="{y+h-9}" font-size="11" text-anchor="end" fill="#0B7285" font-weight="600" font-family="sans-serif">&lt;pi&gt;</text>')
    return (x, y, w, h)

def relship(x, y, w, cn, code, attrs):
    """紫色联系框；attrs: [(中文名, 类型, m)]"""
    n = len(attrs)
    h = 22 + max(n, 1) * 18 + 8
    S.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="10" fill="#E9DDF7" stroke="#6741A3" stroke-width="1.1"/>')
    txt(x + w / 2, y + 15, f"{cn}", size=12.5, fill="#3B2A63", anchor="middle", bold=True, halo=False)
    ry = y + 36
    for cn2, tp, m in attrs:
        tail = ' <tspan fill="#6741A3">&lt;M&gt;</tspan>' if m else ''
        S.append(f'<text x="{x+16}" y="{ry}" font-size="11" fill="#333333" font-family="Consolas, Microsoft YaHei, sans-serif">{esc(cn2)} {esc(tp)}{tail}</text>')
        ry += 18
    return (x, y, w, h)

# ---------- 联系线（先画线，后画框，线头压在框下） ----------
# 拥有1：用户账号 —(identifying 1:1)— 患者档案
line([(960, 324), (960, 450), (820, 450)], identifying=True)
line([(700, 450), (250, 450), (250, 640)], identifying=True)
card(968, 348, "1,1"); card(258, 616, "1,1")
# 拥有2：用户账号 —(identifying 1:1)— 治疗师档案
line([(1120, 324), (1120, 450), (1240, 450)], identifying=True)
line([(1360, 450), (1950, 450), (1950, 640)], identifying=True)
card(1128, 348, "1,1"); card(1958, 616, "1,1")
# 负责绑定：患者档案 —(0,n)—(0,n)— 治疗师档案
line([(420, 747), (500, 747), (500, 718), (880, 718)])
line([(1780, 737), (1700, 737), (1700, 718), (1220, 718)])
card(428, 739, "0,n"); card(1740, 729, "0,n")
# 涉及：患者档案 —(0,n)—(0,n)— 康复部位字典
line([(250, 854), (250, 1128), (880, 1128)])
line([(1220, 1140), (1560, 1140)])
card(258, 878, "0,n"); card(1544, 1130, "0,n")

# ---------- 实体（青色） ----------
UA = entity(840, 90, 340, "用户账号", "user_account", [
    ("用户ID", "BIGINT", True, True),
    ("登录名", "VARCHAR(32)", False, True),
    ("密码密文", "CHAR(64)", False, True),
    ("手机号", "VARCHAR(20)", False, False),
    ("邮箱", "VARCHAR(100)", False, False),
    ("角色编码", "VARCHAR(20)", False, True),
    ("状态", "TINYINT", False, True),
    ("创建时间", "DATETIME", False, True),
    ("删除标记", "TINYINT", False, True),
])
PP = entity(80, 640, 340, "患者档案", "patient_profile", [
    ("用户ID", "BIGINT", True, True),
    ("真实姓名", "VARCHAR(50)", False, False),
    ("性别", "CHAR(1)", False, False),
    ("出生日期", "DATE", False, False),
    ("身高cm", "INT", False, False),
    ("体重kg", "INT", False, False),
    ("禁忌说明", "VARCHAR(500)", False, False),
    ("删除标记", "TINYINT", False, True),
])
TP = entity(1780, 640, 340, "治疗师档案", "therapist_profile", [
    ("用户ID", "BIGINT", True, True),
    ("姓名", "VARCHAR(50)", False, False),
    ("职称", "VARCHAR(50)", False, False),
    ("专业特长", "VARCHAR(200)", False, False),
    ("从业年限", "INT", False, False),
    ("执业资质编号", "VARCHAR(100)", False, False),
    ("删除标记", "TINYINT", False, True),
])
RPD = entity(1560, 1130, 340, "康复部位字典", "rehab_part_dict", [
    ("部位编码", "VARCHAR(32)", True, True),
    ("部位名称", "VARCHAR(50)", True, True),
    ("部位大类", "VARCHAR(32)", True, True),
    ("排序号", "INT", False, False),
    ("启用标记", "TINYINT", False, True),
    ("说明", "VARCHAR(200)", False, False),
])

# ---------- 联系（紫色） ----------
relship(700, 430, 120, "拥有", "owns1", [])
relship(1240, 430, 120, "拥有", "owns2", [])
relship(880, 650, 340, "负责绑定", "binds", [
    ("关系类型", "VARCHAR(20)", True),
    ("生效时间", "DATETIME", True),
    ("结束时间", "DATETIME", False),
    ("状态", "VARCHAR(20)", True),
    ("创建人", "BIGINT", True),
])
relship(880, 1080, 340, "涉及", "involves", [
    ("侧别", "CHAR(1)", True),
    ("主部位标记", "TINYINT", False),
    ("确诊日期", "DATE", False),
])

# ---------- 标题 ----------
txt(W / 2, 34, "CDM-01 账户、患者与治疗师关系 —— PowerDesigner CDM 经典 E/R（Merise）表示法预览", size=20, fill="#111111", anchor="middle", bold=True, halo=False)
txt(W / 2, 56, "青色矩形 = 实体；紫色圆角框 = 联系（Relationship）；'负责绑定'、'涉及' 为带属性的联系，即实体化联系（对应示例图中的'选课''课程评价'）", size=12, fill="#777777", anchor="middle", halo=False)

# ---------- 底部注释 ----------
ny, nh = 1340, 148
S.append(f'<rect x="60" y="{ny}" width="2040" height="{nh}" fill="#FAFAFA" stroke="#BBBBBB" stroke-width="0.8" rx="6"/>')
notes1 = [
    "① 紫色圆角框 = 联系（Relationship）；'负责绑定'与",
    "　 '涉及'为带属性的联系（实体化联系），属性直接画在",
    "　 联系框内——与示例图中'选课（成绩）''课程评价'相同。",
    "② '拥有'为标识联系（1:1，实线）：患者/治疗师档案的",
    "　 主标识 用户ID 由用户账号迁入；其余联系为非标识（虚线）。",
]
notes2 = [
    "③ 基数按 PD 惯例：靠近某实体标注的 min,max 表示",
    "　 \"另一端 1 个实例对应本端多少个实例\"。",
    "④ 物理层差异：patient_therapist / patient_rehab_part",
    "　 的 patient_id、therapist_id、created_by 实际指向",
    "　 user_account（见 rehab_platform_pd.sql）；若需",
    "　 binding_id 作独立主键并被 rehab_plan / consulta-",
    "　 tion 引用，应把'负责绑定'改为实体（关联实体）+ 两条 1:n。",
]
yy = ny + 22
for s in notes1:
    txt(80, yy, s, size=11.5, fill="#444444", halo=False); yy += 19
yy = ny + 22
for s in notes2:
    txt(1080, yy, s, size=11.5, fill="#444444", halo=False); yy += 19

svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}" font-family="Microsoft YaHei, PingFang SC, sans-serif">\n' + "\n".join(S) + "\n</svg>\n"
out = r"F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\ER图_CDM-01_PD经典ER表示法预览.svg"
open(out, "w", encoding="utf-8").write(svg)
print("written", out)
