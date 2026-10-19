# -*- coding: utf-8 -*-
"""PowerDesigner CDM 属性清理工具（概念模型规范化）

用法:
    python CDM属性清理工具.py <cdm文件路径> [--apply]

不加 --apply 时只做体检并打印将被删除的属性清单（不写文件）。
加 --apply 时先备份到同目录 _备份/，再原地删除并校验 XML。

判断依据（CDM 只保留业务属性）:
  1. 审计/技术字段: created_at / updated_at / deleted / deleted_at / created_by
     以及中文名「创建时间 / 更新时间 / 删除时间 / 逻辑删除标记 / 创建人」等
  2. 伪属性: 名称或代码为 CONSTRAINT、以 "--" 开头的注释行
保留: 业务属性、实体标识符（主键）、由关系线迁移生成的外键属性。
"""
import re, os, sys, io, shutil, datetime
import xml.etree.ElementTree as ET

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

A = '{attribute}'; C = '{collection}'; O = '{object}'

AUDIT_CODES = {
    'created_at', 'updated_at', 'deleted_at', 'deleted',
    'created_by', 'updated_by', 'create_time', 'update_time',
    'create_by', 'update_by', 'is_deleted', 'del_flag',
}
AUDIT_NAME_KW = ['创建时间', '更新时间', '删除时间', '逻辑删除', '创建人',
                 '更新人', '创建者', '修改者', '删除标记', '创建时间戳']


def at(el, tag):
    n = el.find(A + tag)
    return (n.text or '') if n is not None else ''


def should_drop(name, code):
    n = (name or '').strip()
    c = (code or '').strip()
    if c.lower() in AUDIT_CODES:
        return '审计/技术字段'
    for kw in AUDIT_NAME_KW:
        if kw in n:
            return '审计/技术字段'
    if c.upper() == 'CONSTRAINT' or 'CONSTRAINT' in n.upper():
        return '非属性（约束名占位）'
    if n.startswith('--'):
        return '非属性（注释行）'
    return None


def main(args):
    if not args:
        print(__doc__)
        return
    src = args[0]
    apply = '--apply' in args

    root = ET.parse(src).getroot()
    dataitems = {d.get('Id'): d for d in root.iter(O + 'DataItem') if d.get('Id')}

    plan = []          # (entity, aid, name, code, reason)
    attrs = {}         # aid -> (entity_name, dataitem_ref)
    for ent in root.iter(O + 'Entity'):
        if not ent.get('Id'):
            continue
        ename = at(ent, 'Name')
        ac = ent.find(C + 'Attributes')
        if ac is None:
            continue
        for ea in ac.findall(O + 'EntityAttribute'):
            aid = ea.get('Id')
            d = ea.find(C + 'DataItem')
            ref = d.find(O + 'DataItem').get('Ref') if d is not None else None
            di = dataitems.get(ref)
            nm = at(di, 'Name') if di is not None else ''
            cd = at(di, 'Code') if di is not None else ''
            attrs[aid] = (ename, ref)
            r = should_drop(nm, cd)
            if r:
                plan.append((ename, aid, nm, cd, r))

    print('文件: %s' % os.path.basename(src))
    print('实体数: %d  属性总数: %d' % (
        len([e for e in root.iter(O + 'Entity') if e.get('Id')]),
        len(attrs)))
    print('命中删除规则: %d 个' % len(plan))
    print('-' * 70)
    cur = None
    for ename, aid, nm, cd, r in plan:
        if ename != cur:
            print('\n【%s】' % ename)
            cur = ename
        print('   - %-34s (%s)  [%s]' % (nm, cd, r))
    if not plan:
        print('无需清理')

    if not apply:
        print('\n(体检模式，未修改文件。加 --apply 执行删除)')
        return

    ids = [p[1] for p in plan]
    ref_keep, ref_drop = set(), set()
    for aid, (ename, ref) in attrs.items():
        if ref is None:
            continue
        (ref_drop if aid in ids else ref_keep).add(ref)
    di_drop = ref_drop - ref_keep

    bakdir = os.path.join(os.path.dirname(src), '_备份')
    os.makedirs(bakdir, exist_ok=True)
    stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    bak = os.path.join(bakdir, os.path.basename(src) + '.bak-' + stamp)
    shutil.copy2(src, bak)

    with open(src, 'r', encoding='utf-8', newline='') as f:
        text = f.read()

    def rm(t, tag, oid):
        p = re.compile(r'<' + tag + r' Id="' + oid + r'">.*?</' + tag + r'>\r?\n', re.S)
        return p.subn('', t, count=1)

    na = nd = 0
    for aid in ids:
        text, n = rm(text, 'o:EntityAttribute', aid); na += n
    for did in sorted(di_drop):
        text, n = rm(text, 'o:DataItem', did); nd += n

    if na != len(ids):
        print('!! 属性匹配数不匹配 (%d/%d)，已中止，文件未改动' % (na, len(ids)))
        return

    with open(src, 'w', encoding='utf-8', newline='') as f:
        f.write(text)
    ET.parse(src)
    print('\n完成：删除属性 %d 个，清理孤立 DataItem %d 个' % (na, nd))
    print('备份: %s' % bak)
    print('XML 校验: 通过')


if __name__ == '__main__':
    main(sys.argv[1:])
