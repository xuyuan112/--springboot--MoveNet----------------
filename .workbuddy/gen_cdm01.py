# -*- coding: utf-8 -*-
"""生成 CDM-01 账户、患者与治疗师关系 ER 图（Chen 表示法）SVG。
布局：顶部 user_account；中部 patient_profile / patient_therapist(关联实体) / therapist_profile；
底部 rehab_part_dict(外部，虚线) / patient_rehab_part(关联实体)。"""
import math

W, H = 2400, 1680
S = []

def esc(t):
    return t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

def line(x1, y1, x2, y2, dashed=False, width=1.2, color='#111', opacity=0.55):
    d = ' stroke-dasharray="7,5"' if dashed else ''
    S.append(f'<line x1="{x1:.0f}" y1="{y1:.0f}" x2="{x2:.0f}" y2="{y2:.0f}" stroke="{color}" stroke-width="{width}"{d} opacity="{opacity}"/>')

def ring_angles(n, gaps):
    norm = []
    for a, b in gaps:
        if a < b:
            norm.append((a, b))
        else:
            norm.append((a, 360.0))
            norm.append((0.0, b))
    norm.sort()
    merged = []
    for a, b in norm:
        if merged and a <= merged[-1][1]:
            merged[-1] = (merged[-1][0], max(merged[-1][1], b))
        else:
            merged.append((a, b))
    arcs, start = [], 0.0
    for a, b in merged:
        if a > start:
            arcs.append((start, a))
        start = b
    if start < 360:
        arcs.append((start, 360.0))
    total = sum(b - a for a, b in arcs)
    pts = []
    for i in range(n):
        pos = (i + 0.5) * total / n
        for a, b in arcs:
            if pos <= b - a:
                pts.append(a + pos)
                break
            pos -= b - a
    return pts

def attr(cx, cy, x, y, cn, en, pk=False, fk=False, dashed=False):
    line(cx, cy, x, y, dashed=dashed, opacity=0.35, width=1.0)
    rx = 64 if len(en) >= 19 else (58 if len(en) >= 15 else 52)
    dash = ' stroke-dasharray="5,4"' if dashed else ''
    color = '#888' if dashed else '#111'
    S.append(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{rx}" ry="16" fill="#fff" stroke="{color}" stroke-width="1.1"{dash}/>')
    S.append(f'<text x="{x:.0f}" y="{y-1:.0f}" font-size="11" text-anchor="middle" fill="#333">{esc(cn)}</text>')
    deco = ' text-decoration="underline"' if pk else ''
    suffix = ' FK' if fk else ''
    S.append(f'<text x="{x:.0f}" y="{y+13:.0f}" font-size="9" text-anchor="middle" fill="#777"{deco}>{esc(en + suffix)}</text>')

def entity(cx, cy, cn, en, dashed=False):
    dash = ' stroke-dasharray="7,5"' if dashed else ''
    color = '#888' if dashed else '#111'
    c1 = '#555' if dashed else '#111'
    c2 = '#999' if dashed else '#555'
    S.append(f'<rect x="{cx-95}" y="{cy-28}" width="190" height="56" fill="#fff" stroke="{color}" stroke-width="1.6"{dash}/>')
    S.append(f'<text x="{cx}" y="{cy-3}" font-size="15" font-weight="bold" text-anchor="middle" fill="{c1}">{esc(cn)}</text>')
    S.append(f'<text x="{cx}" y="{cy+17}" font-size="11" text-anchor="middle" fill="{c2}">{esc(en)}</text>')

def diamond(cx, cy, label, w=58, h=26):
    S.append(f'<polygon points="{cx},{cy-h} {cx+w},{cy} {cx},{cy+h} {cx-w},{cy}" fill="#fff" stroke="#111" stroke-width="1.4"/>')
    S.append(f'<text x="{cx}" y="{cy+4}" font-size="13" text-anchor="middle" fill="#111">{esc(label)}</text>')

def card(x, y, t):
    S.append(f'<text x="{x}" y="{y}" font-size="14" font-weight="bold" text-anchor="middle" fill="#111">{t}</text>')

def edge_point(cx, cy, tx, ty, hw=95, hh=28):
    dx, dy = tx - cx, ty - cy
    if dx == 0:
        return cx, cy + (hh if dy > 0 else -hh)
    t = min(abs(hw / dx), abs(hh / dy)) if dy else abs(hw / dx)
    return cx + dx * t, cy + dy * t

def ring_attrs(ec, attrs, r, gaps, dashed=False):
    angs = ring_angles(len(attrs), gaps)
    for (cn, en, pk, fk), a in zip(attrs, angs):
        rad = math.radians(a)
        x, y = ec[0] + r * math.cos(rad), ec[1] + r * math.sin(rad)
        attr(ec[0], ec[1], x, y, cn, en, pk, fk, dashed=dashed)

# ============ 头 ============
S.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" font-family="Microsoft YaHei, PingFang SC, sans-serif">')
S.append(f'<rect width="{W}" height="{H}" fill="#ffffff"/>')
S.append(f'<text x="70" y="45" font-size="21" font-weight="bold" fill="#111">CDM-01 账户、患者与治疗师关系 ER 图（Chen 表示法）</text>')
S.append('<text x="70" y="68" font-size="12" fill="#555">依据 ER优化SQL/cdm_01_account_binding.sql 绘制　|　椭圆内：中文属性名 + 灰色 SQL 列名（下划线=主键，FK=外键）</text>')

# ============ 实体定义 ============
UA = ((1200, 250), [
    ('用户ID', 'user_id', True, False), ('登录名', 'username', False, False),
    ('密码密文', 'password_hash', False, False), ('角色', 'role_code', False, False),
    ('手机号', 'phone', False, False), ('启用标记', 'status', False, False),
    ('最后登录时间', 'last_login_at', False, False), ('创建时间', 'created_at', False, False),
    ('更新时间', 'updated_at', False, False), ('逻辑删除', 'deleted', False, False)])
PP = ((450, 700), [
    ('档案ID', 'patient_profile_id', True, False), ('用户ID', 'user_id', False, True),
    ('真实姓名', 'real_name', False, False), ('性别', 'gender', False, False),
    ('出生日期', 'birth_date', False, False), ('身高cm', 'height_cm', False, False),
    ('体重kg', 'weight_kg', False, False), ('病史摘要', 'medical_history', False, False),
    ('过敏信息', 'allergy_info', False, False), ('禁忌说明', 'contraindication', False, False),
    ('创建时间', 'created_at', False, False), ('更新时间', 'updated_at', False, False),
    ('逻辑删除', 'deleted', False, False)])
TP = ((1950, 700), [
    ('档案ID', 'therapist_profile_id', True, False), ('用户ID', 'user_id', False, True),
    ('真实姓名', 'real_name', False, False), ('职称', 'title', False, False),
    ('专业特长', 'specialty', False, False), ('执业资质编号', 'license_no', False, False),
    ('个人简介', 'introduction', False, False), ('创建时间', 'created_at', False, False),
    ('更新时间', 'updated_at', False, False), ('逻辑删除', 'deleted', False, False)])
PT = ((1200, 700), [  # patient_therapist 关联实体
    ('绑定ID', 'binding_id', True, False), ('患者用户ID', 'patient_id', False, True),
    ('治疗师用户ID', 'therapist_id', False, True), ('关系类型', 'relation_type', False, False),
    ('状态', 'status', False, False), ('生效时间', 'start_at', False, False),
    ('结束时间', 'end_at', False, False), ('创建人', 'created_by', False, True),
    ('创建时间', 'created_at', False, False)])
PRP = ((1200, 1200), [  # patient_rehab_part 关联实体
    ('记录ID', 'patient_rehab_part_id', True, False), ('患者用户ID', 'patient_id', False, True),
    ('康复部位编码', 'rehab_part_code', False, True), ('侧别', 'side', False, False),
    ('主部位标记', 'primary_flag', False, False), ('状态', 'status', False, False),
    ('确诊日期', 'diagnosed_at', False, False), ('备注', 'remark', False, False),
    ('创建时间', 'created_at', False, False), ('更新时间', 'updated_at', False, False),
    ('逻辑删除', 'deleted', False, False)])
RPD = ((450, 1250), [  # rehab_part_dict 外部实体
    ('部位编码', 'code', True, False), ('部位名称', 'name', False, False),
    ('部位大类', 'group_code', False, False), ('排序号', 'sort_no', False, False),
    ('启用标记', 'enabled', False, False), ('创建时间', 'created_at', False, False),
    ('更新时间', 'updated_at', False, False)])

# ============ 联系线（底层） ============
# 1) user_account —拥有— patient_profile：菱形 (790,470)
x1, y1 = edge_point(*UA[0], 790, 470); x2, y2 = edge_point(*PP[0], 790, 470)
line(x1, y1, 790 - 58, 470, width=1.3, opacity=0.8); line(790 + 58, 470, x2, y2, width=1.3, opacity=0.8)
# 2) user_account —拥有— therapist_profile：菱形 (1610,470)
x1, y1 = edge_point(*UA[0], 1610, 470); x2, y2 = edge_point(*TP[0], 1610, 470)
line(x1, y1, 1610 - 58, 470, width=1.3, opacity=0.8); line(1610 + 58, 470, x2, y2, width=1.3, opacity=0.8)
# 3) profile —负责绑定(n)—[PT]—(m)— therapist（水平线穿过虚线框到菱形）
x1, y1 = edge_point(*PP[0], 1142, 700)
line(x1, y1, 1200 - 80, 700, width=1.3, opacity=0.8)
line(1200 + 80, 700, 1845, 700, width=1.3, opacity=0.8)
# 4) profile —涉及(n)—[PRP]；rehab_part_dict —(m)—[PRP]
x1, y1 = edge_point(*PP[0], 1195, 1176)
line(x1, y1, 1195, 1176, width=1.3, opacity=0.8)
line(1248, 1200, 545, 1247, width=1.3, opacity=0.8)  # 菱形右顶点到 rehab_part_dict 右边

# ============ 属性环 ============
ring_attrs(UA[0], UA[1], 175, [(140, 163), (16, 40), (78, 102)])
ring_attrs(PP[0], PP[1], 250, [(348, 12), (314, 338), (20, 45)])
ring_attrs(TP[0], TP[1], 210, [(202, 226), (168, 192)])
ring_attrs(PT[0], PT[1], 200, [(348, 12), (168, 192), (258, 282)])
ring_attrs(PRP[0], PRP[1], 190, [(245, 269), (78, 102)])
ring_attrs(RPD[0], RPD[1], 160, [(348, 12)], dashed=True)

# ============ 实体矩形 ============
entity(*UA[0], '用户账号', 'user_account')
entity(*PP[0], '患者档案', 'patient_profile')
entity(*TP[0], '治疗师档案', 'therapist_profile')
entity(*RPD[0], '康复部位字典', 'rehab_part_dict', dashed=True)

# 关联实体虚线框（菱形在框内）
S.append(f'<rect x="1050" y="630" width="300" height="140" fill="none" stroke="#555" stroke-width="1.3" stroke-dasharray="8,5"/>')
S.append(f'<rect x="1080" y="1130" width="240" height="140" fill="none" stroke="#555" stroke-width="1.3" stroke-dasharray="8,5"/>')

# ============ 菱形与基数 ============
diamond(790, 470, '拥有');   card(1090, 308, '1'); card(538, 662, '1')
diamond(1610, 470, '拥有');  card(1310, 308, '1'); card(1870, 662, '1')
diamond(1200, 700, '负责绑定', w=80, h=30); card(850, 682, 'n'); card(1690, 682, 'm')
diamond(1200, 1200, '涉及', w=48, h=24);    card(810, 912, 'n'); card(870, 1232, 'm')
S.append('<text x="1200" y="795" font-size="11" text-anchor="middle" fill="#555">patient_therapist（关联实体）</text>')
S.append('<text x="1200" y="1306" font-size="11" text-anchor="middle" fill="#555">patient_rehab_part（关联实体）</text>')

# ============ 关联实体 → user_account 虚线（三元组指向用户主体） ============
S.append('<line x1="1200" y1="630" x2="1200" y2="430" stroke="#888" stroke-width="1.1" stroke-dasharray="7,5" opacity="0.8"/>')
S.append('<text x="1200" y="844" font-size="10.5" text-anchor="middle" fill="#777">患者/治疗师/创建人均指向 user_account.user_id（用户主体，角色由应用层校验）</text>')

# ============ 图例 ============
S.append('<rect x="1560" y="1400" width="800" height="230" fill="#fafafa" stroke="#bbb" stroke-width="1"/>')
S.append('<text x="1580" y="1428" font-size="13" font-weight="bold" fill="#111">图例</text>')
S.append('<rect x="1580" y="1442" width="46" height="22" fill="#fff" stroke="#111" stroke-width="1.4"/><text x="1636" y="1458" font-size="12" fill="#111">实体（矩形）</text>')
S.append('<polygon points="1810,1442 1830,1453 1810,1464 1790,1453" fill="#fff" stroke="#111" stroke-width="1.3"/><text x="1842" y="1458" font-size="12" fill="#111">联系（菱形），旁注 1 / n / m = 基数</text>')
S.append('<ellipse cx="1603" cy="1486" rx="26" ry="11" fill="#fff" stroke="#111" stroke-width="1.1"/><text x="1636" y="1490" font-size="12" fill="#111">属性（椭圆），下划线 = 主键，FK = 外键</text>')
S.append('<rect x="1580" y="1508" width="46" height="22" fill="#fafafa" stroke="#888" stroke-width="1.2" stroke-dasharray="6,4"/><text x="1636" y="1524" font-size="12" fill="#111">外部实体 / 关联实体（虚线框）：跨模块引用见对应 CDM 子图</text>')
S.append('<text x="1580" y="1556" font-size="11" fill="#555">注：patient_therapist 与 patient_rehab_part 为实体化联系（多对多关联实体），其主键即绑定ID/记录ID；</text>')
S.append('<text x="1580" y="1576" font-size="11" fill="#555">patient_therapist 含复合唯一键 uk_binding_id_pair(binding_id, patient_id, therapist_id)，供 rehab_plan /</text>')
S.append('<text x="1580" y="1596" font-size="11" fill="#555">consultation 的复合外键引用（见 CDM-03/CDM-07）。rehab_part_dict 以业务编码 code 为主键（外部引用）。</text>')

S.append('</svg>')

out = r'F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\ER图_CDM-01_账户患者治疗师关系.svg'
with open(out, 'w', encoding='utf-8') as f:
    f.write('\n'.join(S))
print('written:', out)
