# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-25 · 算法·权威化重审 P2 餐次差异化（推荐引擎按餐次期待层挑菜 + per-meal 呈现）。commit bac5dad + 5950a00 已本地提交，未 push。**

## 本 session 交付（算法·权威化重审 P2·会商+UX+Google终审·双绿·commit bac5dad+5950a00 未 push）
- ✅ **算法核心**（shared·引用权威真相源 `DietaryGuideline`·不自创）：`MealCompositionScorer` 加纯函数 `candidateLayers`（`FoodGroup.classify`→宝塔层·永不空集）+ `compositionBonus` 餐次差异分支【①补缺口 +0.5 ②晚餐纯肉轻降 −0.3·**收窄仅纯 ANIMAL 且该餐不期待荤**，不误伤豆腐/豆浆/牛奶/合炒菜】·**默认参不传=旧行为(兼容开关)**。`PeriodPlanner`(精确餐名)/`RecommendationOrchestrator`(单餐早=BREAKFAST/非早=LUNCH) 接餐次期待层。`coveredLayers`/`expectedLayers` 外提出候选循环消热点(Google审建议1)。
- ✅ **UI 呈现**（androidApp·apple_ux 门禁·克制）：推荐页(`AiPlanScreen.DayCard`)每餐名下静态结构建议行(读 `DietaryGuideline.hint`)+页脚口径尾注；今日卡(`DayMealCardView.MealSectionRow`)「缺蔬菜/早餐缺蛋白」阈值提示(鼓励非评判·一处一条·浅灰·可关)；新开关 `MEAL_STRUCTURE_HINT_ENABLED`(默认开·功能设置可关·`rememberMealStructureHintEnabled`)。
- ✅ **测试**：8 P2 纯函数单测 + 1 plan() 集成行为测试(晚餐不以纯肉打头·荤菜数≤午餐·确定性)·旧用例天然验证兼容。
- 验证：`:shared:testDebugUnitTest`(482)+`:androidApp:assembleDebug` **均 BUILD SUCCESSFUL**。Google 质量终审**无阻断项**。
- 文档：`功能总线_权威方法论对照.md`(餐次差异化→✅已权威化)·`待办总览.md`第50行(P2完成+6项P2留待办)·`苹果风格UI设计方案.md §9.41`(可复用范式)。

## ⏭ 下一步
- 【🔴 换 session·会商·L】**P3 推荐搭配份量对齐宝塔** `DietaryGuideline.DAILY_AMOUNTS`（现"一荤一素一汤"自创份量→宝塔各层每日份量）**+ J17 一周计划营养线统一**（抽共享营养规划器·连 J2/摄入建模）。需算法+UX+产品会商。地基(`DAILY_AMOUNTS`)已备。
- 【P2 留待办·下轮·别过度设计】①晚餐"肉偏多"减法/份量阈值判定 ②每餐结构达成度打分 ③整周餐次达成汇总 ④加餐结构建议进今日卡 ⑤建议深浅个性化 ⑥单餐推荐引入目标餐次名(P2.1)。
- 【off-type 队列·各自单开 session】维生素 2b 展示(DB列+.sqm)·库1177营养表分页UI·J22脂肪肝入口·USDA阶段2 cn_en_map扩映射·J20盐"限量非忌口"详情口径。

## 先读清单
1. 本文件 + `feature/功能总线_权威方法论对照.md`（3处自创：色系墙✅/餐次✅/份量🔴待做 + 进度区）
2. `feature/待办总览.md` 第49-50行（权威化重审总纲 + P2 完成/P3 待做）
3. `shared/.../domain/DietaryGuideline.kt`（权威真相源·P3 直接消费 `DAILY_AMOUNTS`）+ `shared/.../ai/MealCompositionScorer.kt`（P2 餐次差异范式·P3 可参照）
4. `CLAUDE.md`（**权威方法论优先准则** + 算法准则A/B + 踩坑红线 + 健康数据/透明/真实红线）

## 工作规则（用户已定·稳定）
1. 🔴 **权威方法论优先**（功能前查权威·别自创·膳食结构走膳食宝塔=`DietaryGuideline`，同"阈值走国标"）· **数据来源真实**。
2. 🔴 **一个 session 只做一类**·off-type 进待办·切类型=换 session·算法/健康改动过**算法+UX 会商门禁**+**Google 质量终审**(阻断必修)别 rush solo。
3. 🔴 构建看输出别信 exit code(grep BUILD SUCCESSFUL)。健康/算法改动过 `:shared:testDebugUnitTest`。**temp/ 未 gitignore→提交显式 add·绝不 git add -A**。
4. 🔴 色系墙红线：**只看膳食结构、不关联热量/慢病**；健康文案守免责·非医嘱·惯例口径标注。
5. 用户要才 push（现有 481c643 + bac5dad + 5950a00 + 本次交接 commit 均**未 push**）。
