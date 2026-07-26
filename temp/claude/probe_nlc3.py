# -*- coding: utf-8 -*-
# [AI生成] 找 nlc 检索入口：抓首页+分类页，定位 food id 与名称的映射路径
import re, requests
HDR = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}

def get(u):
    try:
        r = requests.get(u, timeout=20, headers=HDR)
        r.encoding = 'utf-8'
        return r.status_code, r.text
    except Exception as e:
        return None, f'ERR {type(e).__name__} {str(e)[:80]}'

# 1) 首页找导航链接
for u in ['http://nlc.chinanutri.cn/fq/', 'http://nlc.chinanutri.cn/']:
    sc, t = get(u)
    print('===', u, '->', sc, 'len', len(t) if isinstance(t, str) else t)
    if isinstance(t, str) and sc == 200:
        links = sorted(set(re.findall(r'href=["\']([^"\']*(?:food|fq|search|list|type|class)[^"\']*)["\']', t, re.I)))
        print('相关链接:', links[:30])
        # 表单 action(搜索表单)
        forms = re.findall(r'<form[^>]*action=["\']([^"\']+)["\'][^>]*>', t, re.I)
        print('表单 action:', forms[:10])
        inputs = re.findall(r'<input[^>]*name=["\']([^"\']+)["\']', t, re.I)
        print('输入框 name:', inputs[:15])
        break

# 2) 详情页里找"上一个/下一个/分类"链接(推断 id 遍历可行性)
sc, t = get('http://nlc.chinanutri.cn/fq/foodinfo/442.html')
if isinstance(t, str):
    nav = sorted(set(re.findall(r'href=["\']([^"\']*foodinfo[^"\']*)["\']', t)))
    print('\n详情页内 foodinfo 链接(邻近导航):', nav[:20])
    # 面包屑/分类
    cls = re.findall(r'foodclass[^"\']*', t)
    print('分类链接迹象:', sorted(set(cls))[:10])
