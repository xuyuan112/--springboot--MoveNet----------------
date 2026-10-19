# -*- coding: utf-8 -*-
"""按参考样式绘制《系统功能模块图》七域版：黑白线框树状图，横排模块、竖排叶子，无图题。
七域对齐数据库 CDM 七模块：账户权限 / 动作算法 / 康复计划 / 姿态评估 / 记录报告 / 内容文件 / 咨询运维。"""
from PIL import Image, ImageDraw, ImageFont

HEI = r"C:\Windows\Fonts\simhei.ttf"
f_top = ImageFont.truetype(HEI, 22)
f_mod = ImageFont.truetype(HEI, 17)
f_leaf = ImageFont.truetype(HEI, 14)

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)

modules = [
    ("账户权限模块", ["注册登录", "账号管理", "绑定管理"]),      # M1
    ("动作算法模块", ["动作维护", "规则配置", "模板生成"]),      # M2
    ("康复计划模块", ["计划制定", "计划发布", "版本管理"]),      # M3
    ("姿态评估模块", ["质量门控", "特征计算", "评分纠错"]),      # M4 核心
    ("记录报告模块", ["训练记录", "趋势分析", "报告审核"]),      # M5
    ("内容文件模块", ["内容浏览", "内容收藏", "内容审核"]),      # M6
    ("咨询运维模块", ["在线咨询", "公告管理", "操作日志"]),      # M7
]
sys_name = "基于MoveNet的康复训练辅助平台"

# ---- 布局参数（紧凑化，容纳 7 列）----
LW, LG, MPAD = 36, 12, 14             # 叶子宽 / 叶子间距 / 模块内边距
MW = 3 * LW + 2 * LG + 2 * MPAD       # 模块宽 = 160
MGAP, MARG = 34, 32                   # 模块间距 / 页边距
total_w = len(modules) * MW + (len(modules) - 1) * MGAP + 2 * MARG

TOP_Y, TOP_H = 36, 54
VG1 = 34
MOD_Y, MOD_H = TOP_Y + TOP_H + VG1, 46
VG2 = 34
LEAF_Y = MOD_Y + MOD_H + VG2
LEAF_H = 4 * 18 + 8                    # 4 个竖排字符
H = LEAF_Y + LEAF_H + 30               # 无图题

img = Image.new("RGB", (total_w, H), WHITE)
d = ImageDraw.Draw(img)
LW_LINE = 2


def ctext(cx, cy, s, font):
    bb = d.textbbox((0, 0), s, font=font)
    d.text((cx - (bb[2] - bb[0]) / 2 - bb[0], cy - (bb[3] - bb[1]) / 2 - bb[1]), s, font=font, fill=BLACK)


def box(x, y, w, h):
    d.rectangle([x, y, x + w, y + h], outline=BLACK, width=LW_LINE, fill=WHITE)


def vline(x, y1, y2):
    d.line([x, y1, x, y2], fill=BLACK, width=LW_LINE)


def hline(x1, x2, y):
    d.line([x1, y, x2, y], fill=BLACK, width=LW_LINE)


# ---- 顶部系统框 ----
top_w = 380
top_x = (total_w - top_w) / 2
box(top_x, TOP_Y, top_w, TOP_H)
ctext(total_w / 2, TOP_Y + TOP_H / 2, sys_name, f_top)

# ---- 模块层 ----
mod_centers = []
for i, (mname, leaves) in enumerate(modules):
    mx = MARG + i * (MW + MGAP)
    mc = mx + MW / 2
    mod_centers.append(mc)
    box(mx, MOD_Y, MW, MOD_H)
    ctext(mc, MOD_Y + MOD_H / 2, mname, f_mod)

    # 模块 → 叶子 总线
    leaf_centers = [mx + MPAD + LW / 2 + k * (LW + LG) for k in range(len(leaves))]
    bus_y = MOD_Y + MOD_H + VG2 / 2
    vline(mc, MOD_Y + MOD_H, bus_y)
    hline(min(leaf_centers), max(leaf_centers), bus_y)
    for lc, leaf in zip(leaf_centers, leaves):
        vline(lc, bus_y, LEAF_Y)
        box(lc - LW / 2, LEAF_Y, LW, LEAF_H)
        for j, ch in enumerate(leaf):        # 竖排文字
            ctext(lc, LEAF_Y + 12 + j * 18, ch, f_leaf)

# ---- 顶部 → 模块 总线 ----
bus1_y = TOP_Y + TOP_H + VG1 / 2
vline(total_w / 2, TOP_Y + TOP_H, bus1_y)
hline(min(mod_centers), max(mod_centers), bus1_y)
for mc in mod_centers:
    vline(mc, bus1_y, MOD_Y)

out = r"F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\论文初稿加开题答辩\图4-2 系统功能模块图.png"
img.save(out)
print("saved:", out, img.size)
