# -*- coding: utf-8 -*-
"""按「先 find 后 replace」流程对开题报告执行七处业务域表述替换，避免索引漂移。"""
import json
import subprocess
import sys

SDK = r"F:/腾讯workBuddy/resources/app.asar.unpacked/resources/plugins/workbuddy-builtin/skills/tencent-local-office-edit/edsdk.py"
FID = r"F://基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台//论文初稿加开题答辩//开题报告-基于SpringBoot与MoveNet的康复训练辅助平台.docx"

JOBS = [
    ("六大业务域", "七大业务域"),
    ("（2）康复计划与任务模块。",
     "（2）动作与算法模块。维护标准动作主数据与不可变动作版本，配置关键点、角度阈值与评分权重等评估规则，并生成标准动作模板与采样记录，为姿态评估提供算法依据。（3）康复计划与任务模块。"),
    ("（3）实时姿态评估模块（核心）", "（4）实时姿态评估模块（核心）"),
    ("（4）训练记录与报告模块", "（5）训练记录与报告模块"),
    ("（5）公共内容模块。提供标准动作演示、健康科普等公共康复知识，支持按康复部位、分类和关键词筛选与收藏。",
     "（6）公共内容与文件模块。提供标准动作演示、健康科普等公共康复知识，支持按康复部位、分类和关键词筛选与收藏，附件与演示视频等文件资源由 MinIO 统一存储管理。"),
    ("（6）咨询与通知模块。", "（7）咨询与运维模块。"),
    ("并提供平台公告等通知能力。", "并提供公告发布、操作日志审计与数据备份恢复等平台运维能力。"),
]


def call(tool, args):
    cmd = [sys.executable, SDK, "call", tool, "--json", json.dumps(args, ensure_ascii=False)]
    out = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", cwd=r"F:/腾讯workBuddy/resources/app.asar.unpacked/resources/plugins/workbuddy-builtin/skills/tencent-local-office-edit")
    s = (out.stdout or "") + (out.stderr or "")
    try:
        return json.loads(s)
    except Exception:
        raise RuntimeError(f"{tool} 输出不是 JSON: {s[:300]}")


ok, fail = 0, 0
for old, new in JOBS:
    f = call("doc_find", {"file_id": FID, "text": old})
    locs = f.get("locations", [])
    if len(locs) != 1:
        print(f"[跳过] 命中 {len(locs)} 处：{old[:24]}")
        fail += 1
        continue
    l = locs[0]
    r = call("doc_replace_text", {"file_id": FID, "ranges": [{"begin": l["begin"], "end": l["end"]}], "text": new})
    if r.get("ok", True):
        print(f"[完成] {old[:20]} -> {new[:20]}")
        ok += 1
    else:
        print(f"[失败] {old[:24]}: {r}")
        fail += 1

print("done ok=%d fail=%d" % (ok, fail))
