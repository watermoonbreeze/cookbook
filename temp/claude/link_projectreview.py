# -*- coding: utf-8 -*-
# [AI生成] 把 projectReview 各册里"`NN ...`"形式的册间交叉引用转成可点击 markdown 链接 [NN ...](NN_文件.md)。
import re, glob, os
base = r'D:/Company/Gitee/cookbook/.ai-context/docs/projectReview'
# 册号 → 文件名
files = {os.path.basename(p)[:2]: os.path.basename(p) for p in glob.glob(base + '/*.md')}
# 册号集合(00-08,20)
nums = set(files.keys())
# 匹配反引号包裹、以有效册号开头、其后紧跟 空格/下划线/结束(非数字) 的 span
pat = re.compile(r'`(0[0-8]|20)([ _][^`]*)?`')

def repl(m):
    nn, label = m.group(1), m.group(2) or ''
    if nn not in files:
        return m.group(0)
    inner = nn + label
    return f'[{inner}]({files[nn]})'

total = 0
for p in glob.glob(base + '/*.md'):
    s = open(p, encoding='utf-8').read()
    new, n = pat.subn(repl, s)
    if n:
        open(p, 'w', encoding='utf-8').write(new)
        total += n
        print(f'{os.path.basename(p)}: {n} 处链接化')
print('files:', files)
print('总计', total, '处')
