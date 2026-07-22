# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + 同目录 `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-22 · UI session（餐次简洁化+文案通用化 `339383c` + 营养趋势折线 `6c47381` 均已上线 push。本 UI session 主线两批全清·剩 off-type 队列各单开 session）。**

## 本 session 交付
- ✅ **餐次界面简洁化四改**（`339383c`·§9.39·Google 门禁无阻断）：`MealBlockCard`①低频(选组合/存组合/删本块)收头部 `⋯`ActionSheet ②加菜 添加菜品(主accent)+AI推荐(次中性) weight平分 ③展示区「已选N道」+菜格 与 操作区 半透 hairline 分栏·空块轻引导「还没加菜」 ④常吃chips归操作区首行。删无用 hasCombos。
- ✅ **周计划营养卡文案通用化**（`339383c`）：`NutritionLineCard`「这份计划的营养搭配/整套合计/这几天安排了」(修"吃到了"报告侧措辞口径bug·去"一周"硬编码) + `AiPlanScreen` 空态。
- ✅ **饮食报告·营养走势折线**（`6c47381`·§9.40·apple_ux_designer 门禁出规格+Google 门禁等价性验证无阻断+防漂移单测）：宏量克数单条折线+三色切换chip(默认蛋白)·96dp·min=0归一·断线分段(空天/零值断点)·周画点月不画·锚点轴标·"怎么看"解读行·空/少数据三态·个人视角专属。shared `NutritionTotals.times`+`DietReport.perDayNutrition`(逐日序列作单一真相源·防漂移)。落 `NutritionTrendChart`+`DietReportScreen` 营养摄入卡子区。
- **均真机待验**（餐次:多块/单块无删除/空块/⋯菜单;折线:周/月×个人·断线·切宏量·少数据态）。

## ⏭ 下一步（本 UI session 主线已清空·以下均 off-type·各单开 session）
- 【**数据/编辑**】「菜品食材剂量默认值」专项 = 单位/剂量 bug(详情"青椒100.0个/肉丝无单位"·营养按错单位折算·待办 A#[BUG]行)+加食材智能默认剂量(按分类经验克数非都100g)。**先 adb pull 真机库证实(列名energy_kcal)再改**·数据类单开 session。
- 【**会商**】「是否吃完/实际食用量影响摄入」多方会商出方案(产品+algorithm+UX+行为师+DB·关联北极星营养统计准确性·待办 A#📄行)。
- 【可选·非阻断】折线 Step2(tap 高亮+摘要/切换动效/6主题线色复核)、月视图 x 轴锚点精确定位(Google 建议1·约5%偏差·低优先)、`AiPlanScreen`/VM `weekMacro`/`dayCount` 命名去"week"(Google 建议·触碰时顺手)。

## 队列·off-type（各单开 session·非 UI 呈现）
- 【数据/编辑】「菜品食材剂量默认值」专项 = 单位/剂量 bug(详情"青椒100.0个/肉丝无单位"·营养按错单位折算)+加食材智能默认剂量(按分类经验克数非都100g)·**先 adb pull 真机库证实再改**。
- 【会商】「是否吃完/实际食用量影响摄入」多方会商出方案(数据+算法+产品·关联北极星)。

## 先读清单
1. 本文件 + `待办总览.md`（餐次行 / 新登记 / 点1✅）。
2. 主题方案：`营养趋势曲线方案.md`(曲线·已拍板) / 待办"餐次界面UI优化"行(餐次四改) / `苹果风格UI设计方案.md` §九(§9.37营养呈现/§9.38 Snackbar·§9.39餐次/§9.40曲线 待编码时沉淀)。
3. `CLAUDE.md` 踩坑红线 + 架构准则（凡编码必守）。

## 工作规则（用户已定·稳定）
1. 🔴 **一个 session 只做一类**·off-type 进待办不当场做·切类型=换 session。
2. 中文·深度·无人值守·全权推进·快速做完不反复问确认。每功能过门禁(界面→apple_ux_designer / App自动行为→apple_software_behavior / 文案→copywriter / Android代码→google_quality_engineer)+构建+单测。
3. 🔴 构建看输出别信 exit code（`scripts\build-cli.bat` grep `BUILD SUCCESSFUL`）。崩溃红线：inline Column/Row/Box content lambda 禁 `return@`·coverStyle 变体 if/else。数据 bug 先 python 拉真机库证实再改。
4. 每功能一批→门禁→构建+单测→commit(`[unattended]`)→**push origin/master**→落档。真机验证用户统一做。
5. 红线：健康免责(仅供参考非医嘱)·透明分级告知·联网核准列数据来源·AI生成内容落库·**热量个人概念(受 `CALORIE_NUMBER_ENABLED` 开关·不折算人均)**·抽共享防调参漂移。
