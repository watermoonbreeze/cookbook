# -*- coding: utf-8 -*-
# [AI生成] 外科式追加 50 道新菜到 dishes.json 末尾（保持现有内容字节不变）
import json, io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
P = 'shared/src/commonMain/resources/seed/dishes.json'
NEW = json.load(open('temp/claude/newdish_all.json', encoding='utf-8'))

# 读原文（保留 CRLF，二进制读→utf-8 解码，不动换行）
raw = open(P, 'rb').read().decode('utf-8')

# 定位最后一个 ']'
idx = raw.rstrip().rfind(']')
before = raw[:idx].rstrip()   # 到最后一个元素 '}' 结束（去掉尾部空白/换行）
# before 现在以最后一个对象的 '}' 结尾

# 序列化每个新对象为 2 空格缩进，再整体前缀 2 空格（顶层元素缩进）
def render(obj):
    s = json.dumps(obj, ensure_ascii=False, indent=2)
    return '\n'.join('  ' + line for line in s.split('\n'))

blocks = [render(o) for o in NEW]
appended = ',\n' + ',\n'.join(blocks) + '\n]'
result = before + appended
# 统一换行为 CRLF（steps 内无换行，安全）
result = result.replace('\r\n', '\n').replace('\n', '\r\n')

open(P, 'wb').write(result.encode('utf-8'))

# 回读验证
d = json.load(open(P, encoding='utf-8'))
print('合并后总菜数:', len(d), '(应为 658+50=708)')
print('末尾3道:', [x['name'] for x in d[-3:]])
# 全量唯一性校验
codes = [x['code'] for x in d]
names = [x['name'] for x in d]
assert len(codes) == len(set(codes)), 'code 有重复!'
print('code 唯一性:', 'OK' if len(codes)==len(set(codes)) else 'FAIL')
print('name 重复数:', len(names)-len(set(names)))
