# -*- coding: utf-8 -*-
"""
模块2 CDM 第二轮修正：
1) 删除 3 个 idx_* 物理索引标识符
2) 6 个「主键ID」DataItem 改名为领域化编号（Code 不动，保持与 PDM 对齐）
"""
import io, sys, re, xml.etree.ElementTree as ET

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

P = r'ER优化SQL\Chen氏图模板\模块 2：动作库与姿态评估算法新.cdm'
BAK = P + '.bak-20260921-before-rename'

# 实体 -> 主键新名称
RENAME = {
    '标准动作': '动作编号',
    '动作版本': '版本编号',
    '动作评估规则': '规则编号',
    '标准动作模板': '模板编号',
    'MinIO文件对象': '文件编号',
    '动作模板采样记录': '采样编号',
}

# ---- 锁测试 ----
try:
    f = open(P, 'r+b'); f.close()
except Exception as e:
    print('文件被锁定，请先在 PowerDesigner 中关闭该模型:', e); sys.exit(1)

raw = open(P, 'rb').read()
open(BAK, 'wb').write(raw)          # 备份
has_bom = raw.startswith(b'\xef\xbb\xbf')
text = raw.decode('utf-8-sig')

# ---- 1. 找到每个实体主键属性引用的 DataItem ----
root = ET.fromstring(text.encode('utf-8'))
A = '{attribute}'; O = '{object}'; C = '{collection}'
def at(e, t):
    n = e.find(A + t); return (n.text or '') if n is not None else ''

di2name = {}
for ent in list(root.iter(O + 'Entity')) + list(root.iter(O + 'Association')):
    if not ent.get('Id'): continue
    ename = at(ent, 'Name')
    if ename not in RENAME: continue
    # 主标识符成员即主键属性
    pk = ent.find(C + 'PrimaryIdentifier')
    pk_id = pk.find(O + 'Identifier').get('Ref') if pk is not None else None
    members = []
    if pk_id:
        for ident in ent.find(C + 'Identifiers').findall(O + 'Identifier'):
            if ident.get('Id') == pk_id:
                mc = ident.find(C + 'Identifier.Attributes')
                for ref in mc:
                    members.append(ref.get('Ref'))
    assert len(members) == 1, '%s 主标识符成员数异常: %s' % (ename, members)
    # 该属性指向的 DataItem
    attr = None
    ac = ent.find(C + 'Attributes')
    for ea in ac:
        if ea.get('Id') == members[0]: attr = ea; break
    di2name[attr.find(C + 'DataItem').find(O + 'DataItem').get('Ref')] = RENAME[ename]

print('改名映射:', di2name)

# ---- 2. 文本级改名 DataItem 的 Name/Comment ----
for did, newname in di2name.items():
    m = re.search(r'<o:DataItem Id="%s">(.*?)</o:DataItem>' % did, text, re.S)
    assert m, '未找到 DataItem ' + did
    blk = m.group(0)
    nb = blk.replace('<a:Name>主键ID</a:Name>', '<a:Name>%s</a:Name>' % newname)
    nb = nb.replace('<a:Comment>主键ID</a:Comment>', '<a:Comment>%s</a:Comment>' % newname)
    assert nb != blk, 'DataItem %s 中未找到「主键ID」' % did
    text = text.replace(blk, nb)
    print('  改名: %s -> %s' % (did, newname))

# ---- 3. 删除 3 个 idx_* 标识符块 ----
for idxname in ('idx_action_part_status', 'idx_action_version_status', 'idx_file_purpose'):
    pat = re.compile(r'<o:Identifier Id="(o\d+)">(?:(?!</o:Identifier>).)*?<a:Name>%s</a:Name>.*?</o:Identifier>' % idxname, re.S)
    m = pat.search(text)
    assert m, '未找到标识符 ' + idxname
    text = text[:m.start()] + text[m.end():]
    print('  删除标识符: %s (Id=%s)' % (idxname, m.group(1)))

# ---- 4. 校验 ----
root = ET.fromstring(text.encode('utf-8'))
ents = [e for e in root.iter(O + 'Entity') if e.get('Id')]
assos = [e for e in root.iter(O + 'Association') if e.get('Id')]
rels = [e for e in root.iter(O + 'Relationship') if e.get('Id')]
links = [e for e in root.iter(O + 'AssociationLink') if e.get('Id')]
n_idents = sum(len(e.find(C + 'Identifiers')) for e in ents + assos)
print('\n校验: 实体=%d 关联=%d 关系=%d 关联连线=%d 标识符总数=%d' %
      (len(ents), len(assos), len(rels), len(links), n_idents))
assert len(ents) == 6 and len(assos) == 1 and len(rels) == 4 and len(links) == 2

ids = set(re.findall(r'Id="(o\d+)"', text))
bad = [r for r in set(re.findall(r'Ref="(o\d+)"', text)) if r not in ids]
assert not bad, '悬挂引用: %s' % bad
assert '主键ID' not in text, '仍有「主键ID」残留'

for e in ents + assos:
    ic = e.find(C + 'Identifiers')
    names = [at(i, 'Name') for i in ic.findall(O + 'Identifier')]
    print('  %s: %s' % (at(e, 'Name'), names))

# ---- 5. 写出 ----
out = text.encode('utf-8')
if has_bom:
    out = b'\xef\xbb\xbf' + out
open(P, 'wb').write(out)
print('\n完成: %s (%d 字节)，备份: %s' % (P, len(out), BAK))
