# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-27 · 菜品编辑器主料分级 session**

## 本 session 交付（主料分级·已构建通过·待 commit）

- ✅ **功能评估**：深入探查发现 `is_main` 字段早已存在于 DB/模型/查询中，但编辑器永远写 `false`（toggleMain() 存在但 UI 未接线）——本质是"半拉子工程收尾"而非新功能。
- ✅ **方案决策**：推荐"二进制 isMain + 调料自动标签 + 两组 UI"替代用户最初提出的"主料/辅料/调料三分法"——辅料无独立消费者、调料由分类自动判定、零 schema 迁移。
- ✅ **Apple UX 交互规范**：产出精确到 dp/sp 的分组布局、星标组件、调料 chip、按钮分流、空态全覆盖、保存非阻断校验的完整规范。
- ✅ **编码实现**（2 文件 201+/54−）：
  - VM：`addIngredient` +isMain、`buildAutoDishIngredient` +guessedNames（菜名命中=自动主料）、`applyIngredientGroup` +asMain、`toggleMain` 清 hint、`save()` 非阻断校验、`isSeasoningIngredient()`。
  - UI：食材清单分"主料"/"其他食材"两组、★/☆ 星标 44dp 热区切换、调料自动灰色 chip、添加按钮分流、配料组继承角色、全空态/0主料/全主料全覆盖。
- ✅ **构建+单测**：`:androidApp:assembleDebug` BUILD SUCCESSFUL + `:shared:testDebugUnitTest` 全绿。

## ⏭ 下一步（可接续任务）

- 🥇 **P0 成员化健康红绿灯**（家庭×慢病"可感化"·底座 IntakeCalculator+HealthRuleEngine 已成）→ 当前 session 的主料分级是铺垫（推荐引擎终于能拿到准确主料）。
- 🥇 **P0 引擎正向兑现推荐质量**：主料分级已就绪，可在推荐评估中更准确使用主料信息（主料重复度、主料偏好等）。
- 🔧 可快速验证本 session 改动的真机效果（新建菜品输"土豆牛腩"→土豆/牛腩自动标 ★ 主料）。

## 先读清单
1. 本文件
2. `CLAUDE.md`（规范/门禁/踩坑红线）
3. `功能路径索引.md`（定位先查）
4. `待办总览.md`（任务队列）

## 工作规则（延续）
1. 🔴 权威方法论优先 · 数据来源真实 · 营养免责非医嘱。
2. 🔴 一个 session 聚焦一个内聚任务 · off-type 进待办 · 构建看输出别信 exit code。
3. 🔴 定位先查功能路径索引 · 增删改名文件同 commit 同步索引 · 用户要才 push。
4. 🔴 色系墙只看膳食结构 · 热量数字默认开可关 · 健康文案守免责。
