# -*- coding: utf-8 -*-
"""在论文③系统功能分析各域描述段后插入 用例图+题注（从后往前，避免坐标漂移）"""
import json, subprocess, sys

EDS = r"C:/Users/许袁/.workbuddy/plugins/cache/workbuddy-builtin/skill-tencent-local-office-edit/5.5.3-wb.37748631.g104760a2.h1a8f7c37fe76/edsdk.py"
PY = r"C:/Users/许袁/.workbuddy/binaries/python/versions/3.13.12/python.exe"
TF = r"F:/基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台/论文初稿加开题答辩/毕业论文初稿-基于SpringBoot与MoveNet的康复训练辅助平台.docx"
IMGDIR = r"F:/基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台/论文初稿加开题答辩/毕业论文插图"

# (题注, 图片文件, w, h, 描述段end_index)
TASKS = [
    ("图3-9  公共内容与文件域用例图", "图3-9 公共内容与文件域用例图.png", 560, 304, 16449),
    ("图3-8  数据与报告域用例图",     "图3-8 数据与报告域用例图.png",     580, 242, 16315),
    ("图3-7  姿态评估与训练域用例图", "图3-7 姿态评估与训练域用例图.png", 377, 580, 16166),
    ("图3-6  康复计划域用例图",       "图3-6 康复计划域用例图.png",       560, 417, 15945),
    ("图3-5  动作与算法域用例图",     "图3-5 动作与算法域用例图.png",     580, 270, 15792),
    ("图3-4  账户与权限域用例图",     "图3-4 账户与权限域用例图.png",     580, 287, 15621),
]

def call(tool, payload):
    r = subprocess.run([PY, EDS, "call", tool, "--json", json.dumps(payload, ensure_ascii=False)],
                       capture_output=True, text=True, encoding="utf-8")
    out = (r.stdout or "").strip()
    # 取最后一行 JSON
    line = [l for l in out.splitlines() if l.startswith("{")]
    if not line:
        print("RAW:", out[:300], "| ERR:", (r.stderr or "")[:300]); sys.exit(1)
    return json.loads(line[-1])

for cap, img, w, h, E in TASKS:
    print("=" * 20, cap)
    # 1. 图片追加到描述段末尾
    r1 = call("doc_insert_image", {"file_id": TF, "idx": E,
              "image_path": IMGDIR + "/" + img, "w": w, "h": h})
    assert "ok" in str(r1.get("text", "")), r1
    # 2. 在图片前拆段 → 图片独立成段 [E+1, E+2]
    r2 = call("doc_insert_paragraph", {"file_id": TF, "idx": E})
    # 3. 图片段：居中 + 单倍行距
    call("doc_modify_paragraph", {"file_id": TF,
         "ranges": [{"begin": E + 1, "end": E + 2}],
         "jc": "center", "line_spacing": 1.0, "line_spacing_rule": 1})
    # 4. 图片段：清除首行缩进（与 jc 互斥，单独调）
    call("doc_modify_paragraph", {"file_id": TF,
         "ranges": [{"begin": E + 1, "end": E + 2}], "first_line_indent_chars": 0})
    # 5. 题注段（插在图片段之后）
    r5 = call("doc_insert_paragraph_with_text", {"file_id": TF, "idx": E + 2, "text": cap})
    L = len(cap)  # UTF-16 长度（本例全为 BMP 字符）
    cs, ce = E + 3, E + 3 + L  # 题注段 [cs, ce]
    # 6-7. 题注段：居中 + 无缩进；字体宋体五号
    call("doc_modify_paragraph", {"file_id": TF,
         "ranges": [{"begin": cs, "end": ce}], "jc": "center"})
    call("doc_modify_paragraph", {"file_id": TF,
         "ranges": [{"begin": cs, "end": ce}], "first_line_indent_chars": 0})
    call("doc_update_text_property", {"file_id": TF,
         "ranges": [{"begin": cs, "end": ce - 1}], "font_family": "宋体", "font_size": 10.5})
    print("done", cap, "caption_range", cs, ce)

print("ALL OK")
