# -*- coding: utf-8 -*-
"""生成 CDM-02 动作库与姿态评估算法 ER 图（Chen 表示法）SVG。"""
import math

W, H = 2700, 1680
S = []  # svg fragments

def esc(t):
    return t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

def line(x1, y1, x2, y2, dashed=False, width=1.2, color='#111', opacity=0.55):
    d = ' stroke-dasharray="7,5"' if dashed else ''
    S.append(f'<line x1="{x1:.0f}" y1="{y1:.0f}" x2="{x2:.0f}" y2="{y2:.0f}" stroke="{color}" stroke-width="{width}"{d} opacity="{opacity}"/>')

def ring_angles(n, gaps):
    """在圆环上均匀分布 n 个点（屏幕角度：0=右，90=下，顺时针），跳过 gaps[(a,b)] 区间。
    gaps 可为跨 0 的区间 (a,b) 其中 a>b，如 (348,12) 表示 348°~360° 与 0°~12°。"""
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

def attr(cx, cy, x, y, cn, en, pk=False, fk=False):
    line(cx, cy, x, y, opacity=0.35, width=1.0)
    rx = 64 if len(en) >= 19 else (58 if len(en) >= 15 else 52)
    S.append(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{rx}" ry="16" fill="#fff" stroke="#111" stroke-width="1.1"/>')
    S.append(f'<text x="{x:.0f}" y="{y-1:.0f}" font-size="11" text-anchor="middle" fill="#111">{esc(cn)}</text>')
    deco = ' text-decoration="underline"' if pk else ''
    suffix = ' FK' if fk else ''
    S.append(f'<text x="{x:.0f}" y="{y+13:.0f}" font-size="9" text-anchor="middle" fill="#555"{deco}>{esc(en + suffix)}</text>')

def entity(cx, cy, cn, en):
    S.append(f'<rect x="{cx-95}" y="{cy-28}" width="190" height="56" fill="#fff" stroke="#111" stroke-width="1.6"/>')
    S.append(f'<text x="{cx}" y="{cy-3}" font-size="15" font-weight="bold" text-anchor="middle" fill="#111">{esc(cn)}</text>')
    S.append(f'<text x="{cx}" y="{cy+17}" font-size="11" text-anchor="middle" fill="#555">{esc(en)}</text>')

def diamond(cx, cy, label, w=66, h=26):
    S.append(f'<polygon points="{cx},{cy-h} {cx+w},{cy} {cx},{cy+h} {cx-w},{cy}" fill="#fff" stroke="#111" stroke-width="1.4"/>')
    S.append(f'<text x="{cx}" y="{cy+4}" font-size="13" text-anchor="middle" fill="#111">{esc(label)}</text>')

def card(x, y, t):
    S.append(f'<text x="{x}" y="{y}" font-size="14" font-weight="bold" text-anchor="middle" fill="#111">{t}</text>')

def edge_point(cx, cy, tx, ty, hw=95, hh=28):
    dx, dy = tx - cx, ty - cy
    if dx == 0:
        return cx, cy + (hh if dy > 0 else -hh)
    tx_, ty_ = abs(hw / dx), abs(hh / dy) if dy else float('inf')
    t = min(tx_, ty_)
    return cx + dx * t, cy + dy * t

def ring_attrs(ec, attrs, r, gaps):
    angs = ring_angles(len(attrs), gaps)
    for (cn, en, pk, fk), a in zip(attrs, angs):
        rad = math.radians(a)
        x, y = ec[0] + r * math.cos(rad), ec[1] + r * math.sin(rad)
        attr(ec[0], ec[1], x, y, cn, en, pk, fk)

# ============ 文档头 ============
S.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" font-family="Microsoft YaHei, PingFang SC, sans-serif">')
S.append(f'<rect width="{W}" height="{H}" fill="#ffffff"/>')
S.append(f'<text x="70" y="45" font-size="21" font-weight="bold" fill="#111">CDM-02 动作库与姿态评估算法 ER 图（Chen 表示法）</text>')
S.append('<text x="70" y="68" font-size="12" fill="#555">依据 ER优化SQL/cdm_02_action_algorithm.sql 绘制　|　椭圆内：中文属性名 + 灰色 SQL 列名（下划线=主键，FK=外键）</text>')

# ============ 实体与属性 ============
ENT = {
    'rehab_part_dict': ((320, 280), '康复部位字典', [
        ('部位编码', 'code', True, False), ('部位名称', 'name', False, False),
        ('部位大类', 'group_code', False, False), ('排序号', 'sort_no', False, False),
        ('启用标记', 'enabled', False, False), ('创建时间', 'created_at', False, False),
        ('更新时间', 'updated_at', False, False)]),
    'action': ((950, 280), '标准动作', [
        ('动作ID', 'action_id', True, False), ('动作名称', 'name', False, False),
        ('目标部位编码', 'target_part_code', False, True), ('动作难度', 'difficulty', False, False),
        ('动作描述', 'description', False, False), ('状态', 'status', False, False),
        ('创建人', 'created_by', False, True), ('创建时间', 'created_at', False, False),
        ('更新时间', 'updated_at', False, False), ('逻辑删除', 'deleted', False, False)]),
    'action_version': ((1600, 280), '动作版本', [
        ('动作版本ID', 'action_version_id', True, False), ('标准动作ID', 'action_id', False, True),
        ('版本号', 'version_no', False, False), ('动作类型', 'action_type', False, False),
        ('算法参数快照', 'algo_config', False, False), ('动作签名', 'motion_signature', False, False),
        ('姿态引擎版本', 'pose_engine_id', False, False), ('状态', 'status', False, False),
        ('创建人', 'created_by', False, True), ('创建时间', 'created_at', False, False),
        ('更新时间', 'updated_at', False, False), ('逻辑删除', 'deleted', False, False)]),
    'action_template': ((1500, 820), '标准动作模板', [
        ('模板ID', 'template_id', True, False), ('动作版本ID', 'action_version_id', False, True),
        ('模板来源类型', 'source_type', False, False), ('模板帧率', 'frame_rate', False, False),
        ('模板帧数', 'frame_count', False, False), ('特征维度', 'feature_dim', False, False),
        ('归一化特征序列', 'feature_sequence', False, False), ('关键帧索引', 'keyframe_index', False, False),
        ('样本数量', 'sample_count', False, False), ('状态', 'status', False, False),
        ('创建人', 'created_by', False, True)]),
    'file_object': ((2350, 820), '文件对象', [
        ('文件ID', 'file_id', True, False), ('存储桶', 'bucket_name', False, False),
        ('对象键', 'object_key', False, False), ('原始文件名', 'file_name', False, False),
        ('文件类型', 'content_type', False, False), ('文件大小', 'file_size', False, False),
        ('哈希值', 'sha256', False, False), ('文件用途', 'purpose', False, False),
        ('安全扫描状态', 'scan_status', False, False), ('创建人', 'created_by', False, True),
        ('创建时间', 'created_at', False, False), ('删除时间', 'deleted_at', False, False)]),
    'action_template_sample': ((1500, 1420), '模板采样记录', [
        ('采样ID', 'template_sample_id', True, False), ('模板ID', 'template_id', False, True),
        ('文件ID', 'file_id', False, True), ('采样序号', 'sample_no', False, False),
        ('特征哈希', 'feature_hash', False, False), ('采集时间', 'captured_at', False, False),
        ('创建人', 'created_by', False, True)]),
}

RING = {  # 实体: (半径, 间隙角度区间)
    'rehab_part_dict': (150, [(348, 12)]),
    'action': (220, [(258, 282), (170, 190), (348, 12)]),
    'action_version': (250, [(88, 112), (133, 165), (170, 190), (340, 20)]),
    'action_template': (220, [(228, 292), (348, 12)]),
    'file_object': (210, [(258, 282), (132, 156)]),
    'action_template_sample': (160, [(258, 282), (313, 337), (348, 12)]),
}

# action_rule 双环（23 个属性）
RULE_C = (700, 820)
RULE_INNER = [('规则ID', 'action_rule_id', True, False), ('动作版本ID', 'action_version_id', False, True),
    ('关节名称', 'joint_name', False, False), ('关键点索引', 'keypoint_indices', False, False),
    ('角度计算公式', 'angle_formula', False, False), ('关节角色', 'joint_role', False, False),
    ('评分维度', 'dimension', False, False), ('动作阶段', 'phase', False, False),
    ('最小角度', 'min_angle', False, False), ('最大角度', 'max_angle', False, False),
    ('标准角度', 'standard_angle', False, False)]
RULE_OUTER = [('软容差', 'tolerance_soft', False, False), ('硬容差', 'tolerance_hard', False, False),
    ('保持所需帧数', 'hold_frames_required', False, False), ('最小角速度', 'speed_min', False, False),
    ('最大角速度', 'speed_max', False, False), ('评分权重', 'score_weight', False, False),
    ('纠错严重度', 'severity', False, False), ('纠错提示文案', 'prompt_text', False, False),
    ('对称配对规则ID', 'symmetry_pair_rule_id', False, True), ('创建时间', 'created_at', False, False),
    ('更新时间', 'updated_at', False, False), ('逻辑删除', 'deleted', False, False)]

# 先画属性连线/椭圆（底层），再画实体矩形、菱形、联系线（上层）
for name, (c, cn, attrs) in ENT.items():
    r, gaps = RING[name]
    ring_attrs(c, attrs, r, gaps)
ring_attrs(RULE_C, RULE_INNER, 260, [(160, 200), (317, 341)])
ring_attrs(RULE_C, RULE_OUTER, 390, [(160, 200), (317, 341)])

# 联系线（先画线再压菱形）
def rel(c1, c2, dcx, dcy):
    x1, y1 = edge_point(*c1, dcx, dcy)
    x2, y2 = edge_point(*c2, dcx, dcy)
    line(x1, y1, dcx - 66 if x1 < dcx else dcx + 66, dcy, width=1.3, opacity=0.8)
    line(dcx + 66 if x1 < dcx else dcx - 66, dcy, x2, y2, width=1.3, opacity=0.8)

rel((320, 280), (950, 280), 635, 280)
rel((950, 280), (1600, 280), 1225, 280)
rel((1600, 280), (700, 820), 1152, 585)
rel((1600, 280), (1500, 820), 1560, 605)
rel((1500, 820), (1500, 1420), 1500, 1120)
rel((2350, 820), (1500, 1420), 1928, 1120)

# 实体矩形
for name, (c, cn, attrs) in ENT.items():
    entity(*c, cn, name)
entity(*RULE_C, '动作评估规则', 'action_rule')

# 菱形与基数
diamond(635, 280, '适用');       card(500, 264, '1'); card(775, 264, 'n')
diamond(1225, 280, '拥有版本');  card(1105, 264, '1'); card(1345, 264, 'n')
diamond(1152, 585, '评估规则');  card(1428, 380, '1'); card(1220, 516, 'n')
diamond(1560, 605, '标准模板');  card(1622, 442, '1'); card(1526, 720, 'n')
diamond(1500, 1120, '采样');     card(1534, 990, '1'); card(1534, 1254, 'n')
diamond(1928, 1120, '样本文件'); card(2082, 1005, '1'); card(1778, 1242, 'n')

# action_rule 自关联（对称配对）
S.append('<path d="M 605,802 Q 455,762 283,812" fill="none" stroke="#111" stroke-width="1.3" opacity="0.8"/>')
S.append('<path d="M 283,828 Q 455,878 605,838" fill="none" stroke="#111" stroke-width="1.3" opacity="0.8"/>')
diamond(215, 820, '对称配对', w=70, h=26)
card(440, 772, '0..1'); card(440, 876, '0..1')
S.append('<text x="215" y="756" font-size="11" text-anchor="middle" fill="#555">自关联：须属于同一动作版本</text>')

# 外部引用 user_account（虚线框 + 虚线）
S.append('<rect x="2295" y="135" width="210" height="70" fill="#fafafa" stroke="#888" stroke-width="1.3" stroke-dasharray="7,5"/>')
S.append('<text x="2400" y="164" font-size="14" font-weight="bold" text-anchor="middle" fill="#333">用户账号</text>')
S.append('<text x="2400" y="182" font-size="11" text-anchor="middle" fill="#777">user_account（外部引用，见 CDM-01）</text>')
S.append('<text x="2400" y="197" font-size="10" text-anchor="middle" fill="#999">created_by 指向用户主体</text>')
for pts in [
    '950,252 950,40 2400,40 2400,133',          # action
    '1850,272 2293,196',                         # action_version
    '1595,820 2100,820 2100,170 2293,170',       # action_template
    '2350,608 2350,207',                         # file_object
    '1660,1420 2580,1420 2580,170 2507,170',     # action_template_sample
]:
    p = pts.split(' ')
    d = 'M ' + ' L '.join(p)
    S.append(f'<path d="{d}" fill="none" stroke="#888" stroke-width="1.1" stroke-dasharray="7,5" opacity="0.8"/>')

# 图例
S.append('<rect x="40" y="1450" width="700" height="190" fill="#fafafa" stroke="#bbb" stroke-width="1"/>')
S.append('<text x="60" y="1478" font-size="13" font-weight="bold" fill="#111">图例</text>')
S.append('<rect x="60" y="1492" width="46" height="22" fill="#fff" stroke="#111" stroke-width="1.4"/><text x="116" y="1508" font-size="12" fill="#111">实体（矩形）</text>')
S.append('<polygon points="280,1492 300,1503 280,1514 260,1503" fill="#fff" stroke="#111" stroke-width="1.3"/><text x="312" y="1508" font-size="12" fill="#111">联系（菱形），旁注 1 / n = 基数</text>')
S.append('<ellipse cx="83" cy="1536" rx="26" ry="11" fill="#fff" stroke="#111" stroke-width="1.1"/><text x="116" y="1540" font-size="12" fill="#111">属性（椭圆），下划线 = 主键，FK = 外键</text>')
S.append('<rect x="60" y="1558" width="46" height="22" fill="#fafafa" stroke="#888" stroke-width="1.2" stroke-dasharray="6,4"/><text x="116" y="1574" font-size="12" fill="#111">外部引用实体 / 虚线（跨模块，见对应 CDM 子图）</text>')
S.append('<text x="60" y="1606" font-size="11" fill="#555">注：file_object 为共享实体（完整属性见本图，亦出现于 CDM-00/06/07）；rehab_part_dict 以业务编码 code 为主键，不自增。</text>')
S.append('<text x="60" y="1626" font-size="11" fill="#555">注：action_version 与 action_template 为 1:n（同一版本按 source_type 唯一一份模板）；action_template 与 action_template_sample 为 1:n（按 sample_no 唯一）。</text>')

S.append('</svg>')

out = r'F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\ER图_CDM-02_动作库与姿态评估算法.svg'
with open(out, 'w', encoding='utf-8') as f:
    f.write('\n'.join(S))
print('written:', out)
