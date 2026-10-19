# -*- coding: utf-8 -*-
"""把逻辑模型设计一节的 29 张表设为三线表（顶线/栏目线/底线），并居中表题。"""
import re
import drive

s = drive.structure(60)
nodes = s['nodes']
tabs = [n for n in nodes if n.get('type') == 'Table' and 900 < n['start_index'] < 20300]
caps = [n for n in nodes if n.get('type') != 'Table'
        and 900 < n['start_index'] < 20300
        and re.match(r'^表\d+', (n.get('text_preview') or '').strip())]
print('tables', len(tabs), 'captions', len(caps))

# 1) 表级三线表边框 + 居中
for t in tabs:
    r = drive.call("doc_set_table_properties", {
        "table_id": t['table_id'],
        "alignment": "center",
        "borders": {
            "top": {"val": "single", "sz": 12, "color": "000000"},
            "bottom": {"val": "single", "sz": 12, "color": "000000"},
            "left": {"val": "none"},
            "right": {"val": "none"},
            "inside_h": {"val": "none"},
            "inside_v": {"val": "none"},
        }})
    print(t['table_id'], str(r)[:60])

# 2) 表头行下边框 = 栏目线
for t in tabs:
    cells = [{"row": 1, "col": c,
              "property": {"borders": {"bottom": {"size": 6, "style": "single", "color": "000000"}}}}
             for c in range(1, t['col_count'] + 1)]
    r = drive.call("doc_set_table_cells", {"table_id": t['table_id'], "cells": cells})
    print('hdr', t['table_id'], str(r)[:50])

# 3) 表题：居中
rng = [{"begin": c['start_index'], "end": c['end_index']} for c in caps]
print(drive.call("doc_modify_paragraph", {"ranges": rng, "jc": "center"}))
print(drive.call("doc_update_text_property", {"ranges": rng, "font_size": 10.5}))
