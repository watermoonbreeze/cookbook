# -*- coding: utf-8 -*-
# [AI生成] nlc(中疾控营养所官方)全量交叉核对：对现有食材按名搜 nlc,同中国口径比对 kcal/P/F/C,揪真录入错。零token。
# 接口 FoodInfoQueryAction!queryFoodInfoList.do (categoryOne=0全局+foodName搜)。即写即存JSONL+断点续连+礼貌sleep。
import requests,json,time,re,os
HDR={'User-Agent':'Mozilla/5.0','X-Requested-With':'XMLHttpRequest','Content-Type':'application/x-www-form-urlencoded'}
URL='http://nlc.chinanutri.cn/fq/FoodInfoQueryAction!queryFoodInfoList.do'
base='D:/Company/Gitee/cookbook/temp/claude/nutriverify2/'
OUT=base+'nlc_cross.jsonl'
n=json.load(open('D:/Company/Gitee/cookbook/shared/src/commonMain/resources/seed/ingredient_nutrition.json',encoding='utf-8'))
# 列定义(菠菜验证): [0]id [2]名 [5]食部% [7]能量kJ [8]蛋白g [9]脂肪g [11]纤维g [12]碳水g
def norm(s):  # 去别名[..]、后缀(鲜)(干)、空格
    s=re.sub(r'\[[^\]]*\]','',s); s=re.sub(r'\([^)]*\)','',s); return s.strip().replace(' ','')
def num(s):
    if not s or s in ('—','Tr',''): return None
    m=re.match(r'[-+]?[0-9.]+',str(s)); return float(m.group()) if m else None
def search(name):
    fn=requests.utils.quote(name)
    data=f'categoryOne=0&categoryTwo=0&foodName={fn}&pageNum=1&field=0&flag=0'
    r=requests.post(URL,data=data.encode(),headers=HDR,timeout=20)
    try: return r.json().get('list',[])
    except: return []
def pick(name,lst):
    nn=norm(name); exact=[]; contains=[]
    for o in lst:
        onm=norm(o[2])
        if onm==nn: exact.append(o)
        elif nn in onm or onm in nn: contains.append(o)
    pool=exact or contains
    if not pool: return None
    # 优先"鲜/生"、排除"干/脱水/罐/腌"除非查询名含
    def score(o):
        t=o[2]; s=0
        if '鲜' in t or '生' in t: s+=2
        if any(k in t for k in ['干','脱水','罐','腌','酱']) and not any(k in name for k in ['干','脱水','罐','腌','酱']): s-=3
        return s
    return max(pool,key=score)

done=set()
if os.path.exists(OUT):
    for line in open(OUT,encoding='utf-8'):
        try: done.add(json.loads(line)['name'])
        except: pass
f=open(OUT,'a',encoding='utf-8')
checked=0
for e in n:
    name=e['ingredient']
    if name in done: continue
    try:
        lst=search(name); o=pick(name,lst)
        rec={'name':name,'ours':{'kcal':e.get('kcal'),'P':e.get('protein'),'F':e.get('fat'),'C':e.get('carb')}}
        if o:
            kj=num(o[7]); rec['nlc']={'name':o[2],'kcal':round(kj/4.184,1) if kj else None,
                'P':num(o[8]),'F':num(o[9]),'C':num(o[12]),'fiber':num(o[11]),'edible':o[5]}
        else:
            rec['nlc']=None
        f.write(json.dumps(rec,ensure_ascii=False)+'\n'); f.flush()
        checked+=1
    except Exception as ex:
        f.write(json.dumps({'name':name,'err':str(ex)[:60]},ensure_ascii=False)+'\n'); f.flush()
    time.sleep(0.3)
f.close()
print('checked',checked,'total_done',len(done)+checked)
