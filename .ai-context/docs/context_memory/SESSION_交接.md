# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-25 · 算法·权威化重审 P0+P1 收尾（膳食宝塔权威真相源 + 色系墙评级对齐宝塔四层）。commit 481c643 已本地提交，未 push。**

## 本 session 交付（算法·权威化重审 P0+P1·构建单测双绿·已 commit 481c643 未 push）
- ✅ **P0 权威真相源下沉**：shared 域层新建 `DietaryGuideline`（commonMain·纯 Kotlin）——膳食宝塔五层份量(`DAILY_AMOUNTS`) + 三餐能量分配 + 各餐结构期待(`MealEnergyShare.expectedLayers`) + 食物多样性/饮水，与 UI 层 `DietaryReference` 同源。给出 九大类→宝塔层映射(`LAYER_OF_GROUP`) + `coveredLayers()`。`DietaryGuidelineTest` 7 用例守护。**权威方法论准则落地：算法从此引用它、不再自创。**
- ✅ **P1 色系墙均衡评级权威化**：`FoodGroup.nutritionLevel` 由自创三支柱 → **膳食宝塔四正向层覆盖度**（谷薯/蔬果/鱼禽肉蛋/奶豆坚果·覆盖几层即几级）。**0-4 输出与 label API 不变**→色系墙/餐食卡/今日卡/一周营养线/膳食报告 6 处消费端零改动。`FoodGroupNutritionLevelTest` 同步更新为宝塔层口径。可见影响：奶豆坚果成独立层→单顿正餐"营养优"变少(诚实·全天汇总照样达4)。
- 验证：`:shared:testDebugUnitTest` + `:androidApp:assembleDebug` **均 BUILD SUCCESSFUL**。

## ⏭ 下一步
- 【🔴 换 session·会商+UX】**P2 餐次差异化**：地基(`DietaryGuideline.expectedLayers`)已备，剩**消费侧**——推荐引擎 `MealCompositionScorer`(现零餐次差异·给每餐同等 STAPLE/荤素补分)按餐次差异化(早餐要主食+蛋白+奶/晚餐清淡) + per-meal 呈现。**待办点名"需算法会商+UX"→单开会商 session**(spawn algorithm_engineer + apple_ux_designer)。
- 【🔴 换 session·L】**P3 推荐搭配份量对齐宝塔** + **J17 一周计划营养线统一**（抽共享营养规划器·连 J2/摄入建模·会商）。
- 【off-type 队列】维生素/矿物质 2b 展示(DB列+.sqm+营养表/维生素页)· 库1177营养表分页UI· J22脂肪肝入口· USDA阶段2· J20盐"限量非忌口"详情口径。

## 先读清单
1. 本文件 + `feature/功能总线_权威方法论对照.md`（3处自创现状：色系墙✅已换 / 餐次·份量待会商 + 关键结论进度区）
2. `feature/待办总览.md` 第50行「餐次差异化+三餐能量分配」（含 P0+P1 进度 + P2 消费侧待做）+ J17
3. `shared/.../domain/DietaryGuideline.kt`（权威真相源·P2/P3 直接消费它）+ `FoodGroup.nutritionLevel`（P1 新口径）
4. `CLAUDE.md`（**权威方法论优先准则** + 算法准则A/B + 踩坑红线 + 健康数据/透明/真实红线）

## 工作规则（用户已定·稳定）
1. 🔴 **权威方法论优先**（功能前查权威·别自创·膳食结构走膳食宝塔=`DietaryGuideline`，同"阈值走国标"）· **数据来源真实**。
2. 🔴 **一个 session 只做一类**·off-type 进待办·切类型=换 session·算法/健康改动过**算法+UX 会商门禁**别 rush solo。
3. 🔴 构建看输出别信 exit code(grep BUILD SUCCESSFUL)。健康/算法数据改动过 `:shared:testDebugUnitTest`。**temp/ 未 gitignore→提交显式 add·绝不 git add -A**。
4. 🔴 色系墙红线：**只看膳食结构、不关联热量/慢病**(P1 新口径仍守此)；健康文案守免责·非医嘱。
5. 用户要才 push（现有 481c643 + 本次交接 commit 均**未 push**）。
