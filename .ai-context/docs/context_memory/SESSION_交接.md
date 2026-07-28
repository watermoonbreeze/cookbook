# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-28 · 嘌呤修复+待办整理 session**

## 本 session 交付

- ✅ **K8 嘌呤数据驱动修复**（4文件+2新单测）：草鱼/炖鱼汤痛风红绿灯不一致——嘌呤评估从纯关键词改为数据驱动（与 GI 同模式）。`purineByName()` + `matchPurineByValue()` + `dishQualitativeHits` 加 `purineByName` 参数。构建+shared单测全绿。
- ✅ **DB 拉取分析**：`temp/db/cookbook_20260728_201350.db`，坐实 K6 根因（`buildDishMinis` 缺 `allIngredientNames`）。
- ✅ **真机待验证清单** 落地：`.ai-context/docs/feature/真机待验证清单.md`（49项，分类+验证方法+状态）。
- ✅ **待办总览更新**：K1–K8 八项新需求写入。

## ⏭ 下一步（可接续任务）

- 🔧 **K3 搜索框不清空**（根因已定位·一行修）：`DishPickerViewModel.configure()` 加 `_keyword.value = ""`
- 🔧 **K5 备注不显示**（根因已定位·小修）：`DayMealCardView.MealSectionRow` 加 `section.note` 渲染
- 🔧 **K6 早餐提示误报**（根因已定位·需加查询）：`buildDishMinis` 加 `allIngredientNames` 查询填充
- 🔧 **真机验证**：优先验 P1–P4（搜索/备注/早餐提示/草鱼灯），再验 Phase 2 红绿灯
- 🟡 **K4 去掉上午/下午餐**（DB迁移+多文件·需规划）
- 🟡 **K1 AI快捷输入**（方案已出·待评估）
- 🟡 **K7 营养大类跟随预估**（食材编辑增强）

## 先读清单
1. 本文件
2. `CLAUDE.md`（规范/门禁/踩坑红线）
3. `功能路径索引.md`（定位先查）
4. `待办总览.md`（任务队列·含新增 K1–K8）
5. `真机待验证清单.md`（49项验证追踪·本次新建）
6. `unattended_decisions.md`（历史自主决策）

## 工作规则（延续）
1. 🔴 权威方法论优先 · 数据来源真实 · 营养免责非医嘱。
2. 🔴 一个 session 聚焦一个内聚任务 · off-type 进待办。
3. 🔴 定位先查功能路径索引 · 增删改名文件同 commit 同步索引。
4. 🔴 色系墙只看膳食结构 · 热量数字默认开可关 · 健康文案守免责。
5. 🔴 嘌呤评估数据驱动（与 GI 同模式·别退回纯关键词）。
