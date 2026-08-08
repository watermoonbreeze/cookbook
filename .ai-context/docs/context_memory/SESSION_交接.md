# 🔖 SESSION 交接入口

> 更新时间：**2026-08-08（K1a 营养展示统一化 已实施完毕）**
> **执行模型：CODE@主力机·Claude Code**（实施 K1a 营养统一化全部 STEP + 自测 + 独立 Google 质量终审）。
> 当前状态：**AI记一餐 B4+B5+B6（周期记+NDJSON流式）批次 ACCEPTED，用户仍在真机验证中**。本轮新完成：**K1a 营养展示统一化（蓝图营养部分 STEP-K1A-1.1~3.1 + T-1~2）全部实施 + 三条验收命令全绿 + Google 质量终审无阻断，commit `5c976a49`，`BLUEPRINT_STATE` TURN 已置 ARCH 待复核**。AI 未配置诚实报错（CFG 部分）此前已实施完毕。

---

## 一、本轮完成（AI 引擎标签 + AI→规则自动兜底，含两轮 Google 质量审查）

### 1.1 起因

用户报告：AI 快捷记未配置 AI 时点发送，提示"没能识别出菜品，试试更具体的描述？"——文案误导（暗示是措辞问题，实际是没配 Key）。我最初设计了一个"检测到未配置就同步短路报错+引导去设置"的方案并写入蓝图，但**用户当面纠正**：规则解析应该是 AI 的**兜底**，不是被 AI 挡住的独立模式。正确预期：

1. 标题旁应**始终**显示当前引擎（"AI · 模型名" 或 "规则解析"）。
2. 配置了 AI 时优先调 AI，某段失败要显示失败原因（复用现成的 `CloudAiRuntime` 诚实错误消息）+**自动**（非手动点按钮）回退规则解析，确认页要说明"这是 AI 失败后规则解析的结果"——这个自动回退机制"之前就有"，要求直接复用而非重新设计。

### 1.2 重新设计与实施

按用户要求重新设计后，用户明确选择"我直接写代码实现"（而非走独立 CODE 角色的蓝图交接流程），于是本 session（ARCH）直接完成设计+实现+测试：

- `AiMealInputViewModel` 新增 `configReady`（`activeType()==CLOUD && currentCloudApiKey().isNotBlank()`）+ `refreshEngineStatus()`（由 `AiMealInputSheet` 的 `LaunchedEffect(Unit)` 驱动，覆盖"去设置配置 Key 后返回"场景）+ `engineLabel`（标题旁徽标）。
- 每个 segment 的 AI 侧失败（`LlmStreamEvent.Failed`/流异常/流意外结束/未配置直接跳过 AI）时**自动**触发 `attemptRuleFallback()`，用该段自身的规则解析结果补进 `mergeDays()` 的合并结果；只有 AI 和规则都没解析出内容才是真 ERROR（此时"没能识别出菜品"才是诚实的）。
- 确认页复用既有 `parseSourceMessage` 字段披露"本次结果：规则解析（AI 解析失败：<原因>）"。

### 1.3 两轮独立 Google 质量审查（发现并修复 6 处真实阻断）

**第一轮**发现 5 处 CONFIRMED-ISSUE：
1. `mergeDays()` 按日期字符串（`day.date == seg.targetDate.toString()`）匹配段结果——AI 说"昨天"会给出不同日期，会被误判丢弃整条记录。
2. "流意外结束"分支只比较 `currentSegmentId()`（只看下标不看状态），已成功 COMPLETED 的段仍会触发误判。
3. 内部哨兵/调试字符串（`AI_NOT_CONFIGURED`、`STREAM_COLLECT_ERROR: ...`）未经翻译直接吐进用户可见的"诊断信息"卡片。
4. 自动兜底丢弃了 `RuleFallbackResult.warning`（如"当前餐食以选择的餐食日期为参照"），手动路径原本会展示。
5. 周期记非首段规则兜底的相对日期（`date_offset`）被套用首段（周一）锚点重新解析，日期算错。

**修复**：给 `StreamingMealSession`（shared）新增 `daysForSegment(segmentId)`（按段自身草稿取结果，不靠日期字符串）+ `isStreaming(segmentId)`（真实状态判断）；`mergeDays`/`attemptRuleFallback` 改用这两个新方法；新增 `humanizeWarning()` 过滤/翻译内部代号；新增 `ruleWarnings` 承接规则解析自身的告知；`attemptRuleFallback` 内就近用该段自身 `targetDate` 把相对日期解析成绝对日期。

**第二轮**复核修复效果时，独立审查又发现一处修复引入的**覆盖缺口**："AI 正常 Completed 但没解析出任何菜"（模型只回文字/道歉）这条路径不经过任何 `onFailed` 调用点，第一轮的 `isStreaming` 修复反而让它彻底失去了自动兜底机会——直接卡在 ERROR，且没有任何自动/手动救回路径。**已修复**：在段全部终态后的收尾阶段，对每个仍无合法 AI 结果的段补触发一次 `attemptRuleFallback`（`fallbackAttempted` 保证幂等，不会与已尝试过的段重复）。

### 1.4 验证

新增/重写测试 `T-CFG-01~06`、`T-B3-05`/`T-B3-05b`/`T-B3-07`（重写）、`T-B3-02`（更新断言）。全部验证通过：
- `:androidApp:testDebugUnitTest`（`AiMealInputViewModelStreamTest` 16/16，全量无新增失败）
- `:shared:testDebugUnitTest`（0 failures）
- `:androidApp:assembleDebug`（BUILD SUCCESSFUL）

### 1.5 涉及文件

| 文件 | 改动 |
|---|---|
| `androidApp/.../ui/ai/AiMealInputViewModel.kt` | 核心：`configReady`/`refreshEngineStatus`/`engineLabel`/`attemptRuleFallback`/`mergeDays`/`humanizeWarning`/`ruleWarnings`/收尾兜底补齐 |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | 标题旁引擎标签徽标 + `LaunchedEffect` 刷新 + 溢出防护 |
| `shared/.../ai/meallog/StreamingMealSession.kt` | 新增 `daysForSegment`/`isStreaming` |
| `androidApp/src/test/.../AiMealInputViewModelStreamTest.kt` | 新增/重写共 22 处改动，16 条测试全绿 |
| `.ai-context/docs/feature/AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md` | 顶部追记：原 CFG 设计已否决，指向实际实现 |
| `.ai-context/docs/feature/真机待验证清单_202608080714.md` | 新增 E-CFG-01~06（新建，替换旧时间戳文件） |

### 1.6 本轮新完成（2026-08-08）：K1a 营养展示统一化（commit `5c976a49`，TURN=ARCH 待复核）

按蓝图 §7 营养部分 STEP 机械实施，CODE 角色完成：

- **STEP-K1A-1**：`DishAutoGenerator.preview()` CREATE 分支营养计算从"手写 kcal-only fold"改为复用 `NutritionCalculator.dishNutrition()`（单一真相源，产出蛋白/脂肪/碳水/钠全字段）；新增 `IngredientPreview.toNutritionInput()` 扩展；`anyGuessed` 判定（任一食材 source 非 `Match` → `.copy(estimated=true)`）保证含 Group 均值食材标"（估算）"（INV-K1A-05）。
- **STEP-K1A-2**：`MultiDayRecorder.previewAll()` 新增 REUSE 菜品**批量**营养回填（收集全部 REUSE id → 一次 `nutritionRepo.dishNutrition(reuseIds)` 查询 → 逐层 `.copy()`），避免 N+1（INV-K1A-02）。
- **STEP-K1A-3**：`MealPreviewCard` 手写热量渲染块替换为复用 `DishNutritionLine(dishPreview.nutrition?.toDishNutritionUi())`；删 `calorieOn`/`roundToInt` 死代码。
- **字段替换（GC-11）**：`DishPreview.estimatedKcal: Double?` → `nutrition: DishNutrition?`（3 写入点 + 1 读取点全迁移，`grep estimatedKcal` 生产代码零命中）。
- **新增测试**：`DishAutoGeneratorTest`（T-K1A-01a/b/c/d 4 条）+ `MultiDayRecorderK1aTest`（T-K1A-02/03 2 条，用 `CountingSqlDriver` 数 `selectNutritionInputsByDishIds` SQL 执行次数=1 验零 N+1）。
- **验证**：三条验收命令全绿（shared 674 测试 0 failures；androidApp 全量 0 failures，含 `AiMealInputViewModelStreamTest` 16/16、`GenerationProgressTest` 4/4 两文件零改动仍绿；`assembleDebug` BUILD SUCCESSFUL）。
- **Google 质量终审**（独立 `google_quality_engineer` agent）：**无阻断项**。采纳 2 条🟡（删死函数 `formatQuantity`、`toNutritionInput()` 补同步守卫注释）+ `anyGuessed` 补语义注释（**名字保持蓝图冻结字面量未改名**，完成形态判据依赖）；拒绝 1 条（#3 `inLibrary` 属既有代码、超出本批 allowlist）。

涉及文件：`DishAutoGenerator.kt` / `AutoGenModels.kt` / `MultiDayRecorder.kt`（shared 3）+ `AiMealInputSheet.kt`（androidApp）+ 2 个新测试文件。CFG 部分（AI 未配置诚实报错）非本批、上批已实施。

---

## 二、⏭ 下一步

1. **ARCH 复核 K1a 营养统一化批次**（`BLUEPRINT_STATE` TURN=ARCH，REVIEW=架构师@主力机）：对照蓝图 §9 台账 + `git log dae39fc2..5c976a49` 反查，重点是①冻结字面量约束是否守住（`anyGuessed` 名、grep 判据）②§9 台账与真实 diff 一致性 ③三条验收命令输出已贴（见蓝图 §9）。复核通过则批次关闭、TURN=ACCEPTED；如有阻断项按 `AF-K1A-NN` 记回蓝图。
2. **用户真机验证**：`真机待验证清单_202608081130.md` 的 E-K1A-01（新增，本批）+ E-CFG-01~06（既有）+ 既有 B4/B5/B6 回归项（用户按自己节奏继续）。
3. 已知但上批明确未修的相邻问题（记录于代码注释，非本批范围）：
   - `mergeDays()` 单段仍只取 `firstOrNull` 天——极端场景"一段文字里同时提到昨天和今天两天"只会保留一天，另一天丢失（低概率，未做，Google 质量审查标注为"低 blast radius 的已知限制"）。
   - `humanizeWarning` 未覆盖 `StreamingMealParser` 自身产出的诊断（如"NDJSON 行缺少 segment_id"类消息），这类消息仍可能带工程师黑话/原始 JSON 片段——与本次 3 个哨兵字符串是同一类问题，未来可一并处理。
   - `useRuleFallback()`（手动重试按钮）仍是孤儿方法，无 UI 调用点；本次自动兜底已覆盖其原本要解决的大部分场景，是否还需要接线成按钮，留待后续评估。

---

## 三、关键红线（累加，本轮新增）

- ✅ **本轮新增**：多段自动状态判断（"这段是否已终态/已有合法结果"）禁止用位置指针类 API（如本例的 `currentSegmentId()` 只反映下标顺序）替代真实状态查询——下标不推进 ≠ 没结束，必须用状态本身（如新增的 `isStreaming()`）。
- ✅ **本轮新增**：跨段结果归属禁止用"内容字段值是否等于预期"做匹配（本例：按 `day.date == seg.targetDate.toString()` 字符串匹配）——内容值本身可能被正当地不同于预期（AI declares "昨天"），必须用结构化 ID（segmentId）做归属。
- ✅ **本轮新增**：新增内部诊断代号/哨兵值时，必须同步检查**所有**已有的"用户可见展示"通道（本例漏了 `parseWarnings`，只顾了 `parseSourceMessage`）——同一批新增的调试代号很容易只翻译了其中一条通路。
- ✅ **本轮新增**：为"修复 A 类误判"新增的状态守卫，必须反向核对"是否恰好卡死了 A 类误判曾经附带救回的 B 类真实缺陷"（本例：`isStreaming` 修复了"已成功段被误判"，但同时让"AI 正常结束但没解析出内容"这条路径失去了此前经由该误判分支"顺便"获得的自动兜底机会）——阻断项修复后必须过一遍"这个修复本身是否留下新缺口"，不能止步于"原阻断已消失"。
- ✅ **本轮新增**：用户明确"我直接写代码实现"时，ARCH 角色可以跳过独立 CODE 交接，自行实现+自测；但仍应保留 GC-37 式的独立挑战/审查（本轮用两轮独立 google_quality_engineer agent 替代了"另一个人实现"的效果），不能因为跳过角色分离就跳过独立视角复核。

---

## 四、先读清单（下一 session 接手时按序读）

1. `BLUEPRINT_STATE.md`（确认当前 `TURN`——K1a 营养统一化已实施，TURN=ARCH 待复核）
2. `SESSION_交接.md`（本文件）
3. 需了解 K1a 营养统一化实施细节/验收输出：`.ai-context/docs/feature/AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md` §9（台账+验收命令输出）
4. 需了解既有 AI→规则自动兜底（上批）：同一蓝图文件顶部追记
