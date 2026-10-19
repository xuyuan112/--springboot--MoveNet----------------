# -*- coding: utf-8 -*-
"""操作 实训4 .doc 的驱动脚本：复用 tencent-local-office-edit 的 edsdk 通道。"""
import sys, os, json, time, urllib.request, urllib.error

sys.path.insert(0, r"F:\腾讯workBuddy\resources\app.asar.unpacked\resources\plugins\workbuddy-builtin\skills\tencent-local-office-edit")
import edsdk

FID = "F://毕设实践课程//实训4 数据库设计与系统实现-A3.doc"
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def raw_request(method, params, timeout=300):
    ep = edsdk._discover_endpoint()
    data = json.dumps({"jsonrpc": "2.0", "id": 1, "method": method, "params": params}).encode("utf-8")
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    req = urllib.request.Request(ep, data=data, headers=headers, method="POST")
    with OPENER.open(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def call(tool, args=None, timeout=300, fid=FID):
    a = dict(args or {})
    if fid and "file_id" not in a:
        a["file_id"] = fid
    obj = raw_request("tools/call", {"name": tool, "arguments": a}, timeout=timeout)
    if obj.get("error"):
        return {"ERROR": obj["error"].get("message")}
    r = obj.get("result") or {}
    texts = [c.get("text", "") for c in (r.get("content") or [])
             if isinstance(c, dict) and c.get("type") == "text"]
    body = "\n".join(texts) if texts else json.dumps(r, ensure_ascii=False)
    try:
        return json.loads(body)
    except Exception:
        return body


def structure(preview=80):
    return call("doc_resolve_document_structure",
                {"mode": "compact", "text_preview_length": preview, "limit": 0})


def show(nodes):
    if isinstance(nodes, dict) and "nodes" in nodes:
        nodes = nodes["nodes"]
    for n in nodes:
        if n.get("type") == "Table":
            print(f"{n['paragraph_index']:>4} TABLE id={n['table_id']} [{n['start_index']},{n['end_index']}] {n.get('row_count')}x{n.get('col_count')}")
            continue
        t = (n.get("text_preview") or "")
        if not t.strip():
            continue
        print(f"{n['paragraph_index']:>4} [{n['start_index']},{n['end_index']}] {n.get('type')} {n.get('p_style','')} :: {t[:70]!r}")


def showall(nodes):
    if isinstance(nodes, dict) and "nodes" in nodes:
        nodes = nodes["nodes"]
    for n in nodes:
        if n.get("type") == "Table":
            print(f"{n['paragraph_index']:>4} TABLE id={n['table_id']} [{n['start_index']},{n['end_index']}] {n.get('row_count')}x{n.get('col_count')}")
            continue
        t = (n.get("text_preview") or "")
        print(f"{n['paragraph_index']:>4} [{n['start_index']},{n['end_index']}] {n.get('type')} :: {t[:70]!r}")


def find(text):
    r = call("doc_find", {"text": text})
    return r


def readfile(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def params(name, block):
    p = [(n["start_index"], n["end_index"]) for n in block
         if n.get("type") != "Table"]
    return p
