# -*- coding: utf-8 -*-
"""按角色三端绘制功能模块图：平台 → 患者端/治疗师端/管理端 → 模块 → 功能点（竖排）。不带图题。"""
from PIL import Image, ImageDraw, ImageFont

HEI = r"C:\Windows\Fonts\simhei.ttf"
f_top = ImageFont.truetype(HEI, 24)
f_end = ImageFont.truetype(HEI, 22)
f_mod = ImageFont.truetype(HEI, 21)
f_leaf = ImageFont.truetype(HEI, 24)

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)

# 三端 -> 每端 3 个模块 -> 每模块 3 个功能点
roles = [
    ("患者端", [
        ("账户资料模块", ["注册登录", "资料维护", "安全设置"]),
        ("计划训练模块", ["我的计划", "今日训练", "进度完成"]),
        ("姿态评估模块", ["质量门控", "评分纠错", "计数统计"]),
    ]),
    ("治疗师端", [
        ("患者管理模块", ["患者总览", "档案查看", "异常预警"]),
        ("计划数据模块", ["计划制定", "数据查询", "报告审核"]),
        ("咨询公告模块", ["咨询回复", "草稿确认", "公告查看"]),
    ]),
    ("管理端", [
        ("账号绑定模块", ["账号管理", "绑定关系", "登录日志"]),
        ("动作规则模块", ["动作维护", "规则配置", "模板生成"]),
        ("内容运维模块", ["内容审核", "公告管理", "数据备份"]),
    ]),
]
sys_name = "基于MoveNet的康复训练辅助平台"

# ---- 布局参数 ----
LW, LG, MPAD = 34, 9, 12            # 叶子宽 / 叶子间距 / 模块内边距
LEAF_SPAN = 3 * LW + 2 * LG + 2 * MPAD
MW = LEAF_SPAN                       # 模块宽（4 字标签更窄，由叶子跨度决定）
MGAP, GGAP, MARG = 20, 40, 30        # 模块间距 / 端间距 / 页边距
group_w = 3 * MW + 2 * MGAP
total_w = 3 * group_w + 2 * GGAP + 2 * MARG

TOP_Y, TOP_H = 34, 58
VG1 = 44
END_Y, END_H = TOP_Y + TOP_H + VG1, 50
VG2 = 42
MOD_Y, MOD_H = END_Y + END_H + VG2, 48
VG3 = 42
LEAF_Y = MOD_Y + MOD_H + VG3
LEAF_H = 4 * 27 + 12
H = LEAF_Y + LEAF_H + 30

img = Image.new("RGB", (total_w, H), WHITE)
d = ImageDraw.Draw(img)
W_LINE = 2

def ctext(cx, cy, s, font):
    bb = d.textbbox((0, 0), s, font=font)
    d.text((cx - (bb[2] - bb[0]) / 2 - bb[0], cy - (bb[3] - bb[1]) / 2 - bb[1]), s, font=font, fill=BLACK)

def box(x, y, w, h, lw=W_LINE):
    d.rectangle([x, y, x + w, y + h], outline=BLACK, width=lw, fill=WHITE)

def vline(x, y1, y2, lw=W_LINE):
    d.line([x, y1, x, y2], fill=BLACK, width=lw)

def hline(x1, x2, y, lw=W_LINE):
    d.line([x1, y, x2, y], fill=BLACK, width=lw)

# ---- 顶部系统框 ----
top_w = 420
top_x = (total_w - top_w) / 2
box(top_x, TOP_Y, top_w, TOP_H)
ctext(total_w / 2, TOP_Y + TOP_H / 2, sys_name, f_top)

end_centers = []
for gi, (end_name, mods) in enumerate(roles):
    gx = MARG + gi * (group_w + GGAP)
    ec = gx + group_w / 2
    end_centers.append(ec)
    box(gx, END_Y, group_w, END_H)
    ctext(ec, END_Y + END_H / 2, end_name, f_end)

    for mi, (mname, leaves) in enumerate(mods):
        mx = gx + mi * (MW + MGAP)
        mc = mx + MW / 2
        box(mx, MOD_Y, MW, MOD_H)
        ctext(mc, MOD_Y + MOD_H / 2, mname, f_mod)

        leaf_centers = [mx + MPAD + LW / 2 + k * (LW + LG) for k in range(3)]
        bus_y = MOD_Y + MOD_H + VG3 / 2
        vline(mc, MOD_Y + MOD_H, bus_y)
        hline(min(leaf_centers), max(leaf_centers), bus_y)
        for lc, leaf in zip(leaf_centers, leaves):
            vline(lc, bus_y, LEAF_Y)
            box(lc - LW / 2, LEAF_Y, LW, LEAF_H)
            for j, ch in enumerate(leaf):
                ctext(lc, LEAF_Y + 8 + 13 + j * 27, ch, f_leaf)

    # 端 → 模块 总线
    mod_centers = [gx + mi * (MW + MGAP) + MW / 2 for mi in range(3)]
    bus2 = END_Y + END_H + VG2 / 2
    vline(ec, END_Y + END_H, bus2)
    hline(min(mod_centers), max(mod_centers), bus2)
    for mc in mod_centers:
        vline(mc, bus2, MOD_Y)

# ---- 顶部 → 三端 总线 ----
bus1 = TOP_Y + TOP_H + VG1 / 2
vline(total_w / 2, TOP_Y + TOP_H, bus1)
hline(min(end_centers), max(end_centers), bus1)
for ec in end_centers:
    vline(ec, bus1, END_Y)

out = r"F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\论文初稿加开题答辩\图4-3 系统功能模块图（按角色三端）.png"
img.save(out)
print("saved:", out, img.size)
