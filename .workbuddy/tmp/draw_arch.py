# -*- coding: utf-8 -*-
"""图4-1 系统总体架构图：黑白分层架构，含端侧推理层与七大业务域，无图题。"""
from PIL import Image, ImageDraw, ImageFont

HEI = r"C:\Windows\Fonts\simhei.ttf"
f_lbl = ImageFont.truetype(HEI, 21)   # 层名
f_box = ImageFont.truetype(HEI, 18)   # 模块框
f_sub = ImageFont.truetype(HEI, 15)   # 行小标
f_arr = ImageFont.truetype(HEI, 15)   # 箭头标注

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
LW = 2

W, M = 1500, 30
LBL_W = 118
CX0 = M + LBL_W + 16
CX1 = W - M
CW = CX1 - CX0
PAD = 18
SUB_H = 24
BOX_H = 58
ROW_GAP = 14

layers = [
    ("浏览器层", [
        ("三端页面", ["康复患者端（Vant·移动端）", "康复治疗师端（Element Plus）", "系统管理端（Element Plus）"]),
        ("端侧姿态推理（浏览器本地执行，原始视频与逐帧关键点不出端）",
         ["MoveNet Lightning（ONNX）", "ONNX Runtime Web（WASM多线程）", "质量门控·时序平滑·特征计算", "动作计数·六维评分·纠错提示"]),
    ]),
    ("网关层", [
        (None, ["Nginx（静态资源分发 · 反向代理 · HTTPS · COOP/COEP 跨源隔离响应头）"]),
    ]),
    ("应用层", [
        ("安全与公共支撑", ["JWT 认证", "RBAC+对象级归属", "BCrypt 密码散列", "Redis 缓存与限流", "定时任务调度", "操作日志审计"]),
        ("七大业务域", ["账户与权限", "动作与算法", "康复计划", "姿态评估与训练", "数据与报告", "公共内容与文件", "咨询与运维"]),
    ]),
    ("访问层", [
        (None, ["MyBatis-Plus ORM", "Redis 客户端", "MinIO SDK"]),
    ]),
    ("数据层", [
        (None, ["MySQL 8.0", "Redis 7.2", "MinIO"]),
    ]),
]
ARROWS = ["HTTPS / JSON", "反向代理", "", ""]

img = Image.new("RGB", (W, 1400), WHITE)
d = ImageDraw.Draw(img)


def ctext(cx, cy, s, font):
    bb = d.textbbox((0, 0), s, font=font)
    d.text((cx - (bb[2] - bb[0]) / 2 - bb[0], cy - (bb[3] - bb[1]) / 2 - bb[1]), s, font=font, fill=BLACK)


def ltext(x, y, s, font):
    d.text((x, y), s, font=font, fill=BLACK)


def box(x, y, w, h):
    d.rectangle([x, y, x + w, y + h], outline=BLACK, width=LW, fill=WHITE)


def layer_height(rows):
    h = PAD
    for _, boxes in rows:
        if _ is not None:
            h += SUB_H
        h += BOX_H + ROW_GAP
    return h - ROW_GAP + PAD


def draw_row(y, sub, boxes):
    if sub is not None:
        ltext(CX0 + 10, y + 3, sub, f_sub)
        y += SUB_H
    n = len(boxes)
    gap = 12
    bw = (CW - 2 * 12 - (n - 1) * gap) / n
    for i, t in enumerate(boxes):
        bx = CX0 + 12 + i * (bw + gap)
        box(bx, y, bw, BOX_H)
        ctext(bx + bw / 2, y + BOX_H / 2, t, f_box)
    return y + BOX_H


y = M
GAP = 48
tops = []
for name, rows in layers:
    h = layer_height(rows)
    # 左侧层名
    box(M, y, LBL_W, h)
    ctext(M + LBL_W / 2, y + h / 2, name, f_lbl)
    # 右侧容器
    box(CX0, y, CW, h)
    ty = y + PAD
    for sub, boxes in rows:
        ty = draw_row(ty, sub, boxes) + ROW_GAP
    tops.append((y, y + h))
    y += h + GAP

# 层间双箭头
for i in range(len(tops) - 1):
    y1 = tops[i][1]
    y2 = tops[i + 1][0]
    cx = (CX0 + CX1) / 2
    cy_ = (y1 + y2) / 2
    d.line([cx, y1 + 12, cx, y2 - 12], fill=BLACK, width=LW)
    d.polygon([(cx - 7, y1 + 12), (cx + 7, y1 + 12), (cx, y1 + 2)], fill=BLACK)
    d.polygon([(cx - 7, y2 - 12), (cx + 7, y2 - 12), (cx, y2 - 2)], fill=BLACK)
    if ARROWS[i]:
        bb = d.textbbox((0, 0), ARROWS[i], font=f_arr)
        d.text((cx + 16, cy_ - (bb[3] - bb[1]) / 2 - bb[1]), ARROWS[i], font=f_arr, fill=BLACK)

H = y - GAP + M
img = img.crop((0, 0, W, H))
out = r"F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\论文初稿加开题答辩\毕业论文插图\图4-1 系统总体架构图.png"
img.save(out)
print("saved:", out, img.size)
