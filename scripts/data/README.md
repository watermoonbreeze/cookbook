# 营养数据核对工具集（脚本优先·零 token）

> [AI生成] 2026-07-22。用户原则："联网/权威数据核对一律**脚本优先**，禁把 >50 条长任务逐条丢 LLM agent 联网烧 token"。
> LLM 仅做①中文→英文映射（一次性）②脚本产出的冲突/歧义少数条目裁决③月度抽检。详见 `.ai-context/docs/feature/数据获取脚本化方案.md`。

## 目录内容

| 文件 | 类型 | 说明 |
|---|---|---|
| `cn_usda_map.json` | 数据资产 | 中文食材名→USDA SR Legacy 英文 description 前缀映射（raw 口径优先）。可复用·增量扩充。 |
| `cn_gi_purine_ref.json` | 数据资产 | GI（悉尼库/Atkinson2008）+ 嘌呤（惯例三级口径）参考值表。可复用。 |
| `nutri_selfcheck.py` | 核对工具 | **能量 Atwater 自洽**：kcal≈P×4+F×9+C×4，本地零依赖零 token，最灵敏揪"kcal 与宏量不自洽"的录入错。 |
| `usda_cross.py` | 核对工具 | 现有值 vs USDA 全库交叉。跨口径→多暴露口径差异(干/鲜/加糖/品种)，价值在补缺字段(satFat/矿物质)、揪极端离群。**不盲从他源改中国食材值**。 |
| `nlc_cross.py` | 核对工具 | 现有值 vs nlc(中疾控营养所官方)按名全量交叉。**同中国口径→揪真录入错主力**。 |

## USDA 全库（不入 git·13MB）

`usda_cross.py` 依赖 USDA SR Legacy 全库。重建（一次性）：
```
curl -sL -o usda_sr.zip "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_json_2018-04.zip"
```
再建索引（desc→{kcal/P/F/C/satFat}，nutrient number 208/203/204/205/606）。

## nlc 接口备忘（复用免再侦察）

列表页是 **JS 动态渲染**，数据走 XHR：
```
POST https://nlc.chinanutri.cn/fq/FoodInfoQueryAction!queryFoodInfoList.do   # 2026-07-25: http 已 308→用 https
data: categoryOne=0&categoryTwo=0&foodName={名}&pageNum=1&field=0&flag=0
UA 需伪装浏览器。返回 {list:[[id,img,名,...]]}
列: [0]id [2]名 [5]食部% [7]能量kJ [8]蛋白 [9]脂肪 [11]纤维 [12]碳水  (kcal=kJ÷4.184)
```
`categoryOne=0` 即全局按名搜；空字符串返回 0（无效）。

**nlc_cross.py 常规化用法**（2026-07-25 迭代）：
```
python scripts/data/nlc_cross.py                  # 全量(502·cron 月度体检)
python scripts/data/nlc_cross.py --limit 10       # 前 N 条(抽样验证·友好不硬刷)
python scripts/data/nlc_cross.py --names 糙米,大米  # 指定名(调试匹配)
```
- **别名感知精确匹配**：查询名归一后 == nlc 主名或某括号别名才命中（`稻米[糙米]`→糙米、`黄瓜[胡瓜]`→黄瓜），宁缺毋滥不放宽子串（避免加工/别名错配噪音）。
- 报告落 `reports/nlc_diff_report.md`，缓存 `reports/nlc_cache.jsonl`（断点续连·均 gitignore）。
- **已知 miss 属正常**：nlc 按名搜无该条目（糙米/猪瘦肉返 0·大米只有"香大米"变种）→未来可加"查询词同义扩展(稻米/猪肉)"提命中，但有误配风险，暂不做。

## 交叉核对流程（推荐顺序）

1. **能量自洽** `nutri_selfcheck.py`（本地·最快）→ 揪 kcal 不自洽项
2. **nlc 同口径** `nlc_cross.py` → 揪中国食材真录入错（差异最可信）
3. **USDA 补缺/离群** `usda_cross.py` → 补 satFat/矿物质、揪极端离群
4. 三清单人工只看差异项 → 修真错 → 过四道关（引用完整性+`validateNutritionSeedForTest`+`:shared:testDebugUnitTest`+来源入数据来源页）

## 🔴 红线（脚本只生成报告·不自动改库）

- 脚本**只产出差异/离群报告，绝不自动改 seed**——改数据须人工确认（守透明准则：改数据要告知/可查；守忌口红线：purine 等影响痛风判定的字段用户亲自把关）。
- 跨口径差异（USDA vs 中国成分表）**不盲从他源**：中国成分表是主来源。
- 查不到/口径不确定的字段**省略不编造**；权威来源必入「我的·数据来源」页。

## cron 月度交叉核对（自动化·B 方案）

定时跑上述三源交叉 → 生成差异报告 → 飞书通知"N 处待核" → 人工确认后改。
增量补值同理：新食材缺字段 → 脚本补候选值 → 人工过 → 落库。
（不自动改库，符合红线。）
