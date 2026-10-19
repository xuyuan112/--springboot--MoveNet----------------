# -*- coding: utf-8 -*-
"""对插入的正文/图注/代码段落套用与模板一致的样式（限定在 begin 区间内）。"""
import re, sys
import drive

BLOCK_START, BLOCK_END = 1935, 7920

s = drive.structure(200)
nodes = [n for n in s['nodes'] if n.get('type') != 'Table']

prose, caps, code, imgs = [], [], [], []
for n in nodes:
    b, e = n['start_index'], n['end_index']
    if e < BLOCK_START or b > BLOCK_END:
        continue
    t = (n.get('text_preview') or '').strip()
    if not t:
        continue
    if t.startswith('[Image'):
        imgs.append((b, e))
        continue
    if re.match(r'^(图\d+|代码\d+)', t):
        caps.append((b, e))
        continue
    if all(ord(c) < 128 for c in t):
        code.append((b, e))
        continue
    prose.append((b, e))

print('prose', len(prose), 'caps', len(caps), 'code', len(code), 'imgs', len(imgs))

R = lambda lst: [{"begin": b, "end": e} for b, e in lst]

# 1) 正文段：套模板正文样式 + 首行缩进 2 字符
print(drive.call("doc_modify_paragraph", {"ranges": R(prose), "paragraph_style": "论文正文"}))
print(drive.call("doc_modify_paragraph", {"ranges": R(prose), "first_line_indent_chars": 2}))

# 2) 图注 / 代码题注：居中 + 10.5pt
print(drive.call("doc_modify_paragraph", {"ranges": R(caps), "jc": "center"}))
print(drive.call("doc_update_text_property", {"ranges": R(caps), "font_size": 10.5}))

# 3) 图片段居中
print(drive.call("doc_modify_paragraph", {"ranges": R(imgs), "jc": "center"}))

# 4) 代码段：等宽字体、左对齐、无首行缩进
print(drive.call("doc_update_text_property", {"ranges": R(code), "font_family": "Consolas", "font_size": 10.5}))
print(drive.call("doc_modify_paragraph", {"ranges": R(code), "jc": "left"}))
print(drive.call("doc_modify_paragraph", {"ranges": R(code), "first_line_indent_chars": 0}))
