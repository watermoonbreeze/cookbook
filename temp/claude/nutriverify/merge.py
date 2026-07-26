# -*- coding: utf-8 -*-
# [AI生成] 营养核准结果覆盖升级式合并回 seed
# 用法: python temp/claude/nutriverify/merge.py
# 逻辑: out_shard_*.jsonl 每行=更新营养对象 -> 按 ingredient 名覆盖回源;
#       agent 覆盖的字段生效, agent 省略的字段保留老值; 剔除 _srcnote/_fields_checked 辅助字段;
#       原地更新不改顺序、不新增/删除条目。
import json, glob, os

ROOT = 'shared/src/commonMain/resources/seed/ingredient_nutrition.json'
OUT_DIR = 'temp/claude/nutriverify'
AUX = {'_srcnote', '_fields_checked'}

src = json.load(open(ROOT, encoding='utf-8'))
by_name = {e['ingredient']: e for e in src}

updates = {}
srcnotes = {}
for f in sorted(glob.glob(os.path.join(OUT_DIR, 'out_shard_*.jsonl'))):
    for ln, line in enumerate(open(f, encoding='utf-8'), 1):
        line = line.strip()
        if not line:
            continue
        try:
            obj = json.loads(line)
        except Exception as ex:
            print(f'[跳过] {f}:{ln} 解析失败 {ex}')
            continue
        name = obj.get('ingredient')
        if not name:
            print(f'[跳过] {f}:{ln} 无 ingredient')
            continue
        if name in obj and '_srcnote' in obj:
            srcnotes[name] = obj['_srcnote']
        updates[name] = obj

applied = 0
missing = []
ver_before = sum(1 for e in src if e.get('review') == 'verified')
for name, obj in updates.items():
    if name not in by_name:
        missing.append(name)
        continue
    tgt = by_name[name]
    for k, v in obj.items():
        if k in AUX:
            continue
        if v is None:            # agent 省略/未查到 -> 保留老值
            continue
        tgt[k] = v
    applied += 1

ver_after = sum(1 for e in src if e.get('review') == 'verified')
pend_after = sum(1 for e in src if e.get('review') == 'pending')

json.dump(src, open(ROOT, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
open(ROOT, 'a', encoding='utf-8').write('\n')

print(f'合并条数 {applied} / 更新对象 {len(updates)}')
print(f'verified {ver_before} -> {ver_after}   pending {pend_after}')
if missing:
    print(f'[警告] out 里有源文件不存在的食材名 {len(missing)}: {missing}')
print('srcnote 记录数', len(srcnotes))
json.dump(srcnotes, open(os.path.join(OUT_DIR, '_srcnotes.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
