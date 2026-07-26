# -*- coding: utf-8 -*-
# [AI生成] 侦察 nlc.chinanutri.cn 数据接口：抓详情页，分析数据来自内联/XHR/API
import re
try:
    import requests
except ImportError:
    print("NO_REQUESTS"); raise SystemExit

HDR = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
# 之前 agent 用过的详情页：442=细香葱, 841=酱牛肉
candidates = [
    'http://nlc.chinanutri.cn/fq/foodinfo/442.html',
    'https://nlc.chinanutri.cn/fq/foodinfo/442.html',
]
for u in candidates:
    try:
        r = requests.get(u, timeout=20, headers=HDR)
        print('=== GET', u, '->', r.status_code, 'len', len(r.text))
        t = r.text
        # 找 API/ajax/data 端点
        apis = sorted(set(re.findall(r'(/[a-zA-Z0-9_./]*(?:api|ajax|json|getdata|query|foodinfo|fooddata)[a-zA-Z0-9_./]*)', t, re.I)))
        print('候选接口路径:', apis[:25])
        # 外部 JS
        js = re.findall(r'<script[^>]*src=["\']([^"\']+)["\']', t)
        print('外部JS:', js[:15])
        # 内联数据迹象
        inline = re.findall(r'(var\s+\w+\s*=\s*[\{\[]|window\.__\w+|\.ajax\(|fetch\(|axios)', t)
        print('内联/请求迹象:', inline[:15])
        # 页面里有没有直接的营养数值(说明是服务端渲染)
        has_num = bool(re.search(r'(能量|蛋白质|脂肪|碳水化合物)', t))
        print('含营养字样(服务端渲染?):', has_num)
        print('--- HTML 前 600 字符 ---')
        print(t[:600])
        break
    except Exception as e:
        print('ERR', u, type(e).__name__, str(e)[:120])
