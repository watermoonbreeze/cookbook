# 🔖 SESSION 交接入口

> 更新时间：**2026-08-07（用户装机真机验证 AI记一餐 B4+B5+B6 期间，ARCH 起草下一批蓝图）**
> **执行模型：ARCH@主力机·claude-sonnet-5**（蓝图起草）+ **独立挑战 agent·claude-opus-5**（GC-37 挑战）。
> 当前状态：**AI记一餐 B4+B5+B6（周期记+NDJSON流式）批次 ACCEPTED，用户正在真机验证中，不在本次交接处理范围**。新起草了下一批蓝图——**AI记一餐 K1a 营养展示统一化 + AI 未配置诚实报错**，已完成 ARCH 设计 + 独立 opus agent 的 GC-37 挑战，蓝图状态 `ACCEPTED`，**TURN=CODE**，待实施。

---

## 一、本轮完成（新批次蓝图起草 + GC-37 独立挑战）

### 1.1 触发

用户在真机验证 B4+B5+B6 期间，让 ARCH 并行准备"下一批蓝图规划"；随后用户追加报告一个真实 bug：AI 快捷记未配置 AI 时点发送，提示"没能识别出菜品，试试更具体的描述？"——文案在"未配置"场景下具有误导性。用户明确要求"opus 子智能体来处理"，据此本轮全程用 `model: opus` 的 Explore 型 subagent 做研究/诊断/挑战三段工作，ARCH（主力机）负责综合判断与文档撰写。

### 1.2 研究阶段（opus Explore agent，只读）

一次性完成两部分调研：
- **Part A（bug 根因）**：确认 `SwitchableAiRuntime` 只 override `complete()` 没 override `stream()`，AI 快捷记走的是 `AiRuntime.kt` 默认 `flow{complete(...)}` 包装；未配置 Key 时 `CloudAiRuntime.complete()` 返回 `IllegalStateException("XX API Key 未配置")`，最终被 `handleSessionSnapshot()` 的唯一 ERROR 分支（`AiMealInputViewModel.kt:490-503`）用同一句"没能识别出菜品"文案吞掉，真实原因只沉进次要的 `parseWarnings` 折叠区。VM 构造已注入 `config: AiRuntimeConfig` 但全文件零调用，`isModelReady()` 是现成能力（其他 VM 已用同款方式）。
- **Part B（K1a 现状）**：确认 CREATE 菜品营养计算是独立手写 kcal-only 公式（`DishAutoGenerator.kt:86-91`），REUSE 菜品完全不显营养；`DishNutritionLine`/`NutritionCalculator.dishNutrition()` 现成可复用；K1b 因 L1 合规闸门未完成继续冻结，K1c(weekday) 已实现不需要动。

ARCH 随后又直接读了若干关键文件核实/补全细节（`NutritionCalculator`/`IngredientAutoGenerator`/`MultiDayRecorder`/`AiMealInputSheet` 的精确行号、导航线路等），确保蓝图里的每个技术断言都有真实代码依据。

### 1.3 蓝图起草

产出 `docs/feature/AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md`，L7 颗粒度、37 条 GC 全部逐条勾销（含 N/A 理由）。核心设计：
- CREATE 菜品营养计算改用 `NutritionCalculator.dishNutrition()`（单一真相源，含全部宏量素）。
- REUSE 菜品在 `MultiDayRecorder.previewAll()` 新增**批量**（非 N+1）营养回填。
- `MealPreviewCard` 热量展示换成直接复用既有 `DishNutritionLine`。
- `AiMealInputViewModel.submit()` 新增前置检测，AI 未配置时同步短路，不发起网络请求，给诚实文案 + "去设置" CTA。

### 1.4 GC-37 独立挑战（opus Explore agent，攻击性复核）

本项目蓝图协议的 GC-37 要求"蓝图冻结前必须有独立挑战台账"（正是因为 AF-B456-05 曾经历"设计者=审查者同一人，审不出自己写的规格空隙"）。派出**没有见过起草过程**的独立 opus agent，只给蓝图文件本身，要求逐条核对每一个技术断言、主动找茬。

**结果：14 项挑战，6 项 CONFIRMED-ISSUE（真阻断）、4 项 MINOR-NIT、4 项 CONFIRMED-FINE**。6 项阻断且均已就地修订：

1. `errorKind` 是 `.copy()` 沿用旧值的 sticky 字段——原稿没在 GENERIC 分支显式重写，会导致"未配置→配置好→真解析失败"时 CTA 仍误显"去设置"。**修：`handleSessionSnapshot()` 新增显式写 `GENERIC`**。
2. `DishNutritionLine(null)` 静默不渲染——原稿 `.takeIf{it.hasData}` 会把"算了但没数据"错判成"没算"，该菜整行消失而非显"营养待完善"。**修：去掉过滤，INV 改为区分"从未尝试"vs"尝试但无数据"**。
3. `configReady` 只在 `init{}` 写一次——用户去设置页配置 Key 返回同一 Sheet 后，"去设置"CTA 变死路，功能本身的主流程被堵死。**修：改用 Compose `LaunchedEffect(Unit)` 驱动的 `refreshConfigReady()`**。
4. `init{}` 挂起写入与既有 13 条测试直接构造 VM 后 `submit()` 存在真实竞态，调度不利时全部失败。**修：随第 3 项设计改动自动解决**（新机制下既有测试从不触发刷新，`configReady` 保持默认 `true`，零竞态零改动）。
5. `isModelReady()==false` 同时覆盖用户主动选择的 MOCK（离线规则模式，`AiSettingsScreen` 里可选且已启用）——会把选了离线模式的用户导向一个"其实都配置好了"的设置页，无路可走。**修：判据收窄为 `activeType()==CLOUD && currentCloudApiKey().isBlank()`**。
6. `NutritionInput(unitGrams=1.0)` 恒非空导致 `resolveGrams()` 内建的"估算"判定分支永远走不到，含猜测营养值的菜会**丢失"（估算）"尾注**，看起来像权威数值——违反营养数据诚实性红线。**修：新增显式判定，只要有食材是 Group 均值猜测就强制标记 estimated**。

这次挑战本身就是"多模型协作红线"最好的实证：同一个人（同一次会话）设计的蓝图，独立挑战方几乎总能挑出至少几处真问题——不是因为设计者不认真，而是设计视角天然会对自己的假设视而不见。

---

## 二、⏭ 下一步

**TURN=CODE**。下一 session/机器接手时：

1. 先读 `BLUEPRINT_STATE.md` 确认 `TURN=CODE`。
2. 读 `docs/feature/AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md` 全文（含 §10 独立挑战台账——理解"为什么设计长这样"比只看最终 STEP 更重要，尤其是 §3 INV-CFG-04/05、INV-K1A-04/05 这几条都是挑战后才有的）。
3. 按 §7 分阶段实施步骤顺序机械实现（STEP 均含完成形态字面量 + grep 判据），不自行发挥、不重新设计已被挑战锁定的部分。
4. 完成后填 §9 交付台账 STEP 勾销表，跑 §7 末尾三条验收命令，登记真机清单 `E-K1A-01`/`E-K1A-CFG-01`/`E-K1A-CFG-02`。
5. 交付时到 `docs/experience/14_模型执行力评估.md` 补一行记录实际使用模型名。
6. 完成后 `BLUEPRINT_STATE.md` 的 `TURN` 改回 `ARCH`，等待复核。

**与真机验证的关系**：用户正在验证的是 B4+B5+B6（已 ACCEPTED 的旧批次），与本批次（K1a）无关，两者可并行——CODE 不需要等真机验证结果。

---

## 三、本轮改动文件清单

| 文件 | 改动 |
|---|---|
| `docs/feature/AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md`（新建） | 完整 L7 蓝图，含 §10 独立挑战台账 |
| `docs/context_memory/BLUEPRINT_STATE.md` | 新增当前批次区块（TURN=CODE），历史批次归档保留 |
| `docs/context_memory/SESSION_交接.md` | 本文件（全量重写） |

---

## 四、先读清单（CODE 接手时按序读）

1. `BLUEPRINT_STATE.md`（确认 TURN=CODE）
2. `SESSION_交接.md`（本文件）
3. `docs/feature/AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md` 全文（§0.1 入口 → §1~§9 顺序读，§10 理解设计缘由）

---

## 五、关键红线（累加，本轮未新增项目级红线；蓝图内部红线见该蓝图 §6 显式禁改清单）

沿用既有全部红线（见历史交接记录），本轮额外强调：
- 独立挑战（GC-37）不是形式仪式——本轮 6/14 项挑战是真阻断，直接影响功能可用性（CTA 死路）和数据诚实性（估算标记丢失），**同一人设计+自审的蓝图默认不可信，必须过独立挑战才能冻结**。
- `errorKind` 一类"只在触发分支写、不在恢复/清空分支同步写"的字段，本质是 `.copy()` 语义下的隐性状态机——新增此类字段必须显式列出"phase 转移到该状态的全部入口"并逐个核对是否都写了该字段（本轮 INV-CFG-04 即此教训的沉淀）。
- Compose `init{}` 单次预取 + 直接构造 VM 调用的单测，存在"挂起点未完成时序访问"的真实竞态窗口，不能用"真实竞态窗口≈0"一句话打发——本轮改用"显式刷新函数 + UI 侧 LaunchedEffect 触发"模式规避，值得作为本项目今后同类设计的默认选择。
