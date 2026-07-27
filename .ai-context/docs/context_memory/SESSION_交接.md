# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-27 · 成员红绿灯 Phase 2 session**

## 本 session 交付（成员化健康红绿灯 Phase 2·无人值守·全部完成）

- ✅ **Phase 2-1 列表逐项徽章**（shared+androidApp 6文件）：DishPickerScreen 选菜列表每道菜旁8dp红/黄/绿小圆点，基于当前查看成员约束批量评估。缓存策略：成员约束只查一次+gatherConstraintsForMember + batch SQL loadDishIngredientInfo（免N+1）。无健康约束不显灯。
- ✅ **Phase 2-2 详情全家并集补个人忌口**：DishDetailViewModel.computeInsights() avoid集并上family_member的avoidCategoryIds(展开)+avoidIngredientIds，修单成员场景全家并集行遗漏个人忌口的旧缺陷。
- ✅ **Phase 2-3 成员名可点跳健康档案**：详情页 FamilyVerdictSection 成员名→primary色+clickable→MainScaffold→Family页面。
- ✅ **构建+单测全绿**：`:androidApp:assembleDebug` BUILD SUCCESSFUL + `:shared:testDebugUnitTest` 全绿。
- ✅ **git commit**：`0c14fc5f` [unattended] 10文件 166+/20−。

## ⏭ 下一步（可接续任务）

- 🥇 **P0 引擎正向兑现推荐质量**（多成员忌口贯穿推荐评估打分+忌口引擎扩正向"推有利菜"·本次主料分级+列表徽章是铺垫）
- 🥇 **P0 成员化红绿灯 Phase 3**（可考虑：①菜品列表页DishesScreen+推荐页AiRecommendScreen 也加徽章 ②GI/嘌呤定性纳入列表灯 ③记菜后逐道菜显灯 ④成员点击精确定位该成员编辑器）
- 🔧 **真机验证 Phase 2**：选菜页→有健康档案成员→菜旁显红/黄/绿点；详情页→个人忌口也标红；点成员名→跳到家庭页
- 🟡 **摄入模型占比%呈现**（纯UX修·内部仍存系数·呈现改用归一后占比%）
- 🟡 **F#8 更新基础数据弹窗增量三**（启动弹窗+视觉+文案）

## 先读清单
1. 本文件
2. `CLAUDE.md`（规范/门禁/踩坑红线）
3. `功能路径索引.md`（定位先查）
4. `待办总览.md`（任务队列）
5. `unattended_decisions.md`（本次自主决策明细）

## 工作规则（延续）
1. 🔴 权威方法论优先 · 数据来源真实 · 营养免责非医嘱。
2. 🔴 一个 session 聚焦一个内聚任务 · off-type 进待办 · 构建看输出别信 exit code。
3. 🔴 定位先查功能路径索引 · 增删改名文件同 commit 同步索引 · 用户要才 push。
4. 🔴 色系墙只看膳食结构 · 热量数字默认开可关 · 健康文案守免责。
