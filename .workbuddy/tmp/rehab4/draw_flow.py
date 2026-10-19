# -*- coding: utf-8 -*-
"""绘制姿态评估与训练评分模块处理流程图（双列：实时路径 / 结束路径）。"""
from PIL import Image, ImageDraw, ImageFont

S = 3
W, H = 1100, 830
img = Image.new('RGB', (W * S, H * S), 'white')
d = ImageDraw.Draw(img)

YH = 'C:/Windows/Fonts/msyh.ttc'
YHB = 'C:/Windows/Fonts/msyhbd.ttc'
def F(size, bold=False):
    return ImageFont.truetype(YHB if bold else YH, int(size * S))

LINE = (60, 60, 60)
FILL_START = (232, 238, 247)
FILL_DIA = (255, 246, 229)
FILL_NEW = (227, 240, 253)

def box(x, y, w, h, text, fill='white', font=None, radius=0):
    x0, y0, x1, y1 = (x - w / 2) * S, (y - h / 2) * S, (x + w / 2) * S, (y + h / 2) * S
    if radius:
        d.rounded_rectangle([x0, y0, x1, y1], radius=radius * S, fill=fill, outline=LINE, width=int(1.7 * S))
    else:
        d.rectangle([x0, y0, x1, y1], fill=fill, outline=LINE, width=int(1.7 * S))
    f = font or F(12.5)
    lines = text.split('\n')
    th = sum(d.textbbox((0, 0), ln, font=f)[3] - d.textbbox((0, 0), ln, font=f)[1] + 6 * S for ln in lines)
    cy = y * S - th / 2
    for ln in lines:
        bb = d.textbbox((0, 0), ln, font=f)
        d.text((x * S, cy - bb[1]), ln, font=f, fill=(20, 20, 20), anchor='ma')
        cy += (bb[3] - bb[1]) + 6 * S

def diamond(x, y, w, h, text, font=None):
    pts = [(x * S, (y - h / 2) * S), ((x + w / 2) * S, y * S), (x * S, (y + h / 2) * S), ((x - w / 2) * S, y * S)]
    d.polygon(pts, fill=FILL_DIA, outline=LINE, width=int(1.7 * S))
    box(x, y, w, h, text, fill=None, font=font) if False else None
    f = font or F(12)
    d.text((x * S, y * S), text, font=f, fill=(20, 20, 20), anchor='mm')

def arrow(pts, label=None, label_pos=None, label_anchor='mm'):
    for i in range(len(pts) - 1):
        (ax, ay), (bx, by) = pts[i], pts[i + 1]
        d.line([ax * S, ay * S, bx * S, by * S], fill=LINE, width=int(1.7 * S))
    (ax, ay), (bx, by) = pts[-2], pts[-1]
    import math
    ang = math.atan2(by - ay, bx - ax)
    L, WW = 11, 4.6
    tipx, tipy = bx * S, by * S
    p1 = (tipx - L * S * math.cos(ang) + WW * S * math.sin(ang), tipy - L * S * math.sin(ang) - WW * S * math.cos(ang))
    p2 = (tipx - L * S * math.cos(ang) - WW * S * math.sin(ang), tipy - L * S * math.sin(ang) + WW * S * math.cos(ang))
    d.polygon([(tipx, tipy), p1, p2], fill=LINE)
    if label:
        lx, ly = label_pos
        f = F(11)
        bb = d.textbbox((lx * S, ly * S), label, font=f, anchor=label_anchor)
        d.rectangle([bb[0] - 4 * S, bb[1] - 2 * S, bb[2] + 4 * S, bb[3] + 2 * S], fill='white')
        d.text((lx * S, ly * S), label, font=f, fill=(20, 20, 20), anchor=label_anchor)

def title(x, y, text):
    f = F(13.5, True)
    bb = d.textbbox((x * S, y * S), text, font=f, anchor='mm')
    d.rectangle([bb[0] - 6 * S, bb[1] - 3 * S, bb[2] + 6 * S, bb[3] + 3 * S], fill='white')
    d.text((x * S, y * S), text, font=f, fill=(20, 20, 20), anchor='mm')

# ---------- 双列坐标 ----------
LX, LW = 225, 355          # 左列中心 / 宽
RX, RW = 880, 395          # 右列中心 / 宽
BWH, BH = 60, 46           # 节距 / 高
Y0 = 130

left = [
    ('start', '开始：患者进入训练任务页'),
    ('p', '请求动作快照（规则 / 算法参数 / 标准模板）'),
    ('p', '校验姿态引擎版本与模型摘要'),
    ('p', '摄像头采集视频帧'),
    ('new', 'MoveNet 推理 → 17 个人体关键点'),
    ('p', '入场检查、质量门控与关键点平滑修复'),
    ('p', '计算关节角度与左右对称差'),
    ('p', '阶段状态机计数（双阈值滞回、去抖）'),
    ('p', '实时渲染骨架、动作计数与纠错提示'),
    ('start', '训练结束：原始帧缓存不出浏览器'),
]
right = [
    ('new', '重放本地缓存的原始帧（不上传）'),
    ('p', '序列级修复与特征重建'),
    ('p', '状态机重放，切分动作周期'),
    ('p', '动作签名校验（角度 / 时长 / 方向）'),
    ('new', '六维加权评分 + DTW 相似度'),
    ('p', '姿态质量判定（有效帧不足则不计分）'),
    ('p', '生成训练汇总（评分、次数、纠错摘要）'),
    ('new', '提交 POST /training-records（幂等落库）'),
    ('start', '结束：写入训练记录表'),
]

title(LX, 70, '实时路径（逐帧、因果：即时反馈）')
title(RX, 70, '结束路径（非因果、确定性复算：最终成绩）')

ly = {}
for i, (kind, text) in enumerate(left):
    y = Y0 + i * BWH
    ly[i] = y
    fill = FILL_START if kind == 'start' else (FILL_NEW if kind == 'new' else 'white')
    box(LX, y, LW, BH, text, fill=fill, radius=BH / 2 if kind == 'start' else 0)
    if i:
        arrow([(LX, ly[i - 1] + BH / 2), (LX, y - BH / 2)])

ry = {}
for i, (kind, text) in enumerate(right):
    y = Y0 + i * BWH
    ry[i] = y
    fill = FILL_START if kind == 'start' else (FILL_NEW if kind == 'new' else 'white')
    box(RX, y, RW, BH, text, fill=fill, radius=BH / 2 if kind == 'start' else 0)
    if i:
        arrow([(RX, ry[i - 1] + BH / 2), (RX, y - BH / 2)])

# 循环回路：实时渲染 -> 采集视频帧
lx_out = LX + LW / 2
arrow([(lx_out, ly[8]), (470, ly[8]), (470, ly[3]), (lx_out + 2, ly[3])])
d.text((436 * S, ((ly[8] + ly[3]) / 2) * S), '每帧循环', font=F(11), fill=(20, 20, 20), anchor='mm')

# 左列末节点 -> 右列首节点
arrow([(LX + LW / 2, ly[9]), (610, ly[9]), (610, ry[0]), (RX - RW / 2 + 2, ry[0])])
d.text((628 * S, ((ly[9] + ry[0]) / 2) * S), '训练结束 → 结束路径', font=F(11), fill=(20, 20, 20), anchor='lm')

# 裁白
img = img.resize((W, H), Image.LANCZOS)
bb = img.convert('L').point(lambda v: 0 if v > 248 else 255).getbbox()
img = img.crop((bb[0] - 16, bb[1] - 16, bb[2] + 16, bb[3] + 16))
out = 'flow.png'
img.save(out)
print('saved', out, img.size)
