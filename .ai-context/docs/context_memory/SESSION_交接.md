# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-25 · 算法·权威化重审 P3 份量对齐（呈现补齐+口径收敛·克数不做）。改动已实现+双绿+Google终审无阻断，尚未 commit。**

## 本 session 交付（算法·权威化重审 P3·会商算法+UX+产品拍板"呈现补齐+口径收敛"·Google终审无阻断·双绿·未提交）
- ✅ **③ 口径收敛(shared·核心权威化)**：周计划营养线 `NutritionLine` 缺口口径从自创三支柱→**膳食宝塔四正向层**(`pillarGapDays→layerGapDays`·`GAP_PILLAR→GAP_LAYER`·引用 `DietaryGuideline` 单一真相源)。新增能力=识别"顿顿有肉却不喝奶不吃豆"的**奶豆坚果缺层**。`balanceScore` 连贯分分母 3→4(`POSITIVE_LAYERS.size`)。advise 显式按宝塔层序 tie-break(Google审🟡1·不依赖 Map 迭代序)。全 App 只剩一套宝塔结构口径(消灭三支柱 vs 宝塔漂移)。
- ✅ **① 单餐入口补齐(androidApp)**：库存/随机推荐(`AiRecommendScreen`)结果区顶部补餐次结构建议行(`MealStructureHintRow`·与 AiPlan 逐字同款·选定具体餐次显·"全部"不显)——这就是"份量对齐覆盖所有推荐入口"。
- ✅ **② 权威份量轻入口(androidApp)**：推荐页+周期计划页脚"各类每天吃多少 ›"(`DailyAmountRefLink`·文字跳转)→膳食参考依据页(权威 `DAILY_AMOUNTS` 可达不喧宾)。`MainScaffold` wire `onOpenDietaryReference`。
- 🔴**明确不做**(会诊否决·数据不可靠+焦虑+免责红线)：克数选菜(C)、份量达标进度条/达成度%、减法说教。**权威每日克数份量只在参考页详列，高频推荐动线绝不铺克数。**
- 验证：`:shared:testDebugUnitTest`(含新增"肉菜饭无奶豆→识别奶豆坚果缺层"核心测试)+`:androidApp:assembleDebug` **均 BUILD SUCCESSFUL**。Google 质量终审**无阻断项**(评"质量高于项目平均")。
- 文档已更：`功能总线_权威方法论对照.md`(三自创点全✅)·`待办总览.md`第50行(P3完成+8项P2/P3留待办)·`苹果风格UI设计方案.md §9.42`·`周计划营养线方案.md`(P3口径收敛注)。

## ⏭ 下一步
- 【🔴 未提交】本 session 改动**尚未 git commit**。用户确认后按"呈现补齐+口径收敛"提交(shared 3文件+androidApp 3文件+4文档·**temp/ 不 add**)。
- 【权威化重审已收官三大自创点】P1色系墙/P2餐次/P3份量 全✅。**剩 🟡 深化项**(对照文档·各单开 session·会商)：EER个人热量校准(DRIs)·生命阶段适配推荐(孕/老/儿)·食物多样性提示(12种/25种)·微量营养素评估扩展(铁钙维D)。
- 【P2+P3 留待办·下轮·别过度设计】晚餐肉偏多减法·每餐结构达成度·整周达成汇总·今日卡加"各类每天吃多少"轻入口(需Home plumbing)·WeekPlan已排周缺层回归断言(Google审🟡2·低优)·单餐引入目标餐次名。

## 先读清单
1. 本文件 + `feature/功能总线_权威方法论对照.md`（三自创点全✅ + 进度区）
2. `feature/待办总览.md` 第49-50行（权威化重审总纲 + P0~P3 全完成/🟡深化待做）
3. `shared/.../domain/DietaryGuideline.kt`（权威真相源）+ `NutritionLine.kt`/`NutritionLineAdvisor.kt`（P3 口径收敛范式）+ `ai/MealCompositionScorer.kt`（P2 范式）
4. `CLAUDE.md`（权威方法论优先准则 + 算法准则A/B + 踩坑红线 + 健康数据/透明/真实红线）

## 工作规则（用户已定·稳定）
1. 🔴 **权威方法论优先**（功能前查权威·别自创·膳食结构走膳食宝塔=`DietaryGuideline`）· **数据来源真实**。
2. 🔴 **一个 session 聚焦一个内聚任务**（含数据+界面等多面·如 P3 算法+呈现是两面一起做·用户2026-07-25修正）·off-type 的**另一个任务**进待办·算法/健康改动过**算法+UX 会商门禁**+**Google 质量终审**(阻断必修)。
3. 🔴 构建看输出别信 exit code(grep BUILD SUCCESSFUL)。健康/算法改动过 `:shared:testDebugUnitTest`。**temp/ 未 gitignore→提交显式 add·绝不 git add -A**。
4. 🔴 色系墙红线：**只看膳食结构、不关联热量/慢病**；健康文案守免责·非医嘱·惯例口径标注。份量克数只在参考页、不进高频动线。
5. 用户要才 push（现有 481c643+bac5dad+5950a00+7521c98 及本次 P3 均**未 push**）。
