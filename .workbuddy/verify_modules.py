# -*- coding: utf-8 -*-
"""逐列对比 ER优化SQL 模块文件与主表 rehab_platform_pd.sql 的同名列，输出差异。"""
import re, glob

def parse(path):
    tables = {}
    for m in re.finditer(r'CREATE TABLE (\w+) \(\n(.*?)\n\)[^\n]*\n', open(path, encoding='utf-8').read(), re.DOTALL):
        t, body = m.group(1), m.group(2)
        cols = []
        for line in body.split('\n'):
            line = line.strip()
            if re.match(r'^\w+\s+(BIGINT|VARCHAR|CHAR|INT|TINYINT|DATE|DATETIME|TEXT|LONGTEXT|JSON|DECIMAL)', line):
                cols.append(line.split()[0])
        tables[t] = cols
    return tables

main = parse(r'F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\rehab_platform_pd.sql')
for f in sorted(glob.glob(r'F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\ER优化SQL\*.sql')):
    name = f.split('\\')[-1]
    for t, cols in parse(f).items():
        if t not in main:
            print(f'{name}: {t} 不在主表中！')
            continue
        miss = [c for c in main[t] if c not in cols]
        extra = [c for c in cols if c not in main[t]]
        if miss or extra:
            print(f'{name} [{t}] 缺列: {miss or "无"} | 多列: {extra or "无"}')
print('对比完成')
