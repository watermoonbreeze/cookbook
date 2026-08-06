# AI记一餐：周期记 + NDJSON 流式 B4 输入 UI 实施蓝图

> 状态：`ACCEPTED` —— 2026-08-06 由 Claude（Google 质量+架构+Apple UX 三角色联合设计）通过，开始 B4 编码。
> **🔴 架构模型复核检查点**：B4 蓝图 + B4 编码实现需由架构模型（google_architecture_engineer + apple_architect）从本检查点开始全量审核。复核范围 = 本蓝图全部 + B4 全部代码改动 + 测试证据。入口：本文 §0 门禁表 + §3 不变量 + §7 最小改动集 + §9 测试矩阵。
> 起草日期：2026-08-06
> 前置蓝图：`AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md`（AF-ARCH-01/02 已关闭，AF-ARCH-03 由本蓝图 §1 冻结）
> 必读：`AI记一餐_周期记_NDJSON流式开发规范.md` §2.1/§5.3、`AI记一餐_周期记_NDJSON流式改造落地方案.md` §四/§五、`experience/12_多模型协作与实施蓝图规范.md`、B3 蓝图 §11.7（ChatGPT 复核 4 项门禁）、`苹果风格UI设计方案.md` §九
> 基线：`a7fdf074`（B1+B2+B3 全部生产代码 + 645 tests 0 failures）

---

## §0. B4 编码前门禁（ChatGPT 复核要求，逐项勾销）

| # | 门禁 | 本蓝图落点 | 状态 |
|---| ---- | --------- | :--: |
| 1 | B4 蓝图第一步冻结 AF-ARCH-03：N 次独立请求 × 每次 1 段 | §1 | ✅ 已冻结 |
| 2 | 原审核 S1~S6 逐条写入蓝图，注明采纳方案或不采纳原因 | §8 | ✅ 已逐条处理 |
| 3 | 确认 645 项测试结果运行在完整 SHA `a7fdf074...` 上 | — | ✅ 已确认（见 SESSION_交接.md） |
| 4 | AF-ARCH-02 边界检查表（7 项）写入 B4 蓝图不变量 | §2 不变量表 | ✅ 已纳入 |

---

## §1. AF-ARCH-03 冻结：请求段数策略

### 决策

**采用「N 次独立请求 × 每次 1 段」。**

### 理由

1. **与 AF-ARCH-02 一致**：每个 segment 已有独立 parser（`segmentParsers: LinkedHashMap`），每次请求只传 1 段 → parser 的 `segments.size` 恒为 1 → 整体 JSON fallback 始终有效。
2. **支持按段重试**：某段失败可单段重试，不影响已完成段。
3. **支持按段进度**：UI 可展示"第 3/7 天生成中"。
4. **失败隔离**：一段的截断/解析错误不污染其他段。
5. **取消粒度**：用户可中途取消剩余段，已生成内容保留。

### 具体动作（本蓝图 §5 实施脚本第 0 步，B4 编码第一步）

| 动作 | 文件 | 内容 |
|------|------|------|
| 收窄 `buildStreamingRequest` 签名 | `AiMealPrompt.kt` | `fun buildStreamingRequest(segment: InputSegment): LlmRequest`（单段，非 List） |
| 删除多段 prompt 分支 | `AiMealPrompt.kt` | 删除 `buildStreamingUserPrompt` 中 `if (segments.size == 1) ... else ...` 的 else 分支（周期记多天拼装逻辑） |
| 删除 maxTokens 缩放 | `AiMealPrompt.kt` | `maxTokens` 固定为 2048（单段），删除 `nonBlank.size * 4096` 缩放 |
| 更新调用点 | `AiMealInputViewModel.kt` | `AiMealPrompt.buildStreamingRequest(seg)` 已传单段，仅需确认编译通过 |

### 不变

- `StreamingMealSession` 的 `orderedSegments` 遍历逻辑**不改**——它已经正确处理多段串行（`nextSegment()` 逐个取、`while` 循环逐个请求）。
- ViewModel 的 `submit()` 循环**不改**——它已经是"每段一次 `aiRuntime.stream()` + `collect`"。
- 唯一变化是 `buildStreamingRequest` 不再接受 `List`，调用方本就传 `listOf(seg)`，改为传 `seg` 即可。

---

## §2. 目标、非目标与范围冻结

### 目标

将 `AiMealInputSheet` 的输入区从"单一文本输入框"改造为"**快速记 / 周期记**双模式输入"，支持周期记的连续日期段选择与每日独立输入。`AiMealInputViewModel.submit()` 从"构造单个 quick segment"扩展为"按输入模式构造 1 个或 N 个 segment"。

### 非目标

- **B5 确认页流式展示**（渐进卡片、进度条、截断/失败诊断、最终重排）——B5 另立蓝图
- **B6 收尾**（文档、真机清单、全量审查）
- 数据库 schema、SQLDelight 迁移
- DI/Koin 重构
- 健康建议行为变更
- 语音输入行为变更（快速记/周期记共用既有语音逻辑）
- Prompt 协议变更（已在 §1 AF-ARCH-03 中完成签名收窄，不改变 NDJSON_SYSTEM_PROMPT 内容）
- `StreamingMealSession` / `StreamingMealParser` 生产代码（已在 B3+AF-ARCH-02 中就位）

### Allowlist（仅允许修改的文件）

| 文件 | 允许操作 | 禁止操作 |
|------|---------|---------|
| `shared/.../ai/meallog/InputSegmentFactory.kt`（**新建**） | 纯函数：`forQuickRecord()` / `forPeriodicRecord()` / `mondayOfWeek()` | 访问 UI、Repository、数据库、网络 |
| `shared/.../ai/meallog/InputSegmentFactoryTest.kt`（**新建**） | 测试周一边界、跨年周、空白段过滤、charCount | — |
| `shared/.../ai/meallog/AiMealPrompt.kt` | 收窄 `buildStreamingRequest` 签名为单段 + 删多段分支 | 改 NDJSON_SYSTEM_PROMPT 内容、改 FLAT_SYSTEM_PROMPT |
| `androidApp/.../ui/ai/AiMealInputViewModel.kt` | 新增 `InputMode` 字段到 UiState、`submit()` 按模式构造 segments、`setInputMode()`/`setWeekInputText()` 等新动作 | 改 B3 的 generation 编排/状态机/保存/健康摘要逻辑 |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | InputPhase 内部重构：加 SegmentedControl、周期记日期列表+每日输入框、200 字限制 UI | 改 ParsingPhase/PreviewPhase/保存/关闭守卫 |
| `androidApp/.../test/.../AiMealInputViewModelStreamTest.kt` | 新增周期记多段测试 | 改既有 T-B3-01~09 |
| `shared/.../test/.../InputSegmentFactoryTest.kt`（**新建**） | 工厂纯单测 | — |
| `.ai-context/docs/...` | 更新 B4 台账、真机清单 | — |

**禁止修改：** `StreamingMealSession`、`StreamingMealParser`、`MealStreamDraftMapper`、`CloudAiRuntime`、`StreamTransport`、`CloudAiRequestConfig`、Repository、SQLDelight、Gradle、DI、B1/B2/B3 已有测试。

---

## §3. 不变量

### 3.1 B4 新增不变量

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|----|------|---------|---------|------|
| INV-B4-01 | 用户选择"周期记"模式 | 展示连续日期列表（默认当前周 Mon~Sun），每天一个独立输入框，各 ≤200 字 | 共用同一个输入框、日期不可选 | T-B4-01 |
| INV-B4-02 | 周期记模式点击发送 | 仅为非空白段构造 `InputSegment`（`segmentId="week-{weekAnchor}-day{N}"`），按 `ordinal` 升序排列；`weekAnchor` = 所选周周一 | 发送空白段、segmentId 格式错误、ordinal 乱序 | T-B4-02 |
| INV-B4-03 | 快速记模式点击发送 | 构造单个 `InputSegment`（`segmentId="quick-{targetDate}"`），行为与 B3 完全一致 | 周期记格式的 segmentId | T-B4-03 |
| INV-B4-04 | 切换输入模式（快速记 ↔ 周期记） | 各自保留草稿不丢失；切换回原模式时恢复原输入 | 切换清空对方草稿 | T-B4-04 |
| INV-B4-05 | 周期记某天输入超过 200 字 | 截取前 200 字（`take(200)`），并给出一次性可见提示 | 静默截断、阻止输入、发送超限文本 | T-B4-05 |
| INV-B4-06 | 粘贴内容到周期记某天输入框 | 超过 200 字截取并提示；未超限直接粘贴 | 静默截断 | T-B4-05 |
| INV-B4-07 | 周期记所有天均为空白时点击发送 | 发送按钮禁用（与快速记一致：`inputText.isBlank()` → 不可发送） | 发送空请求 | T-B4-02 |
| INV-B4-08 | `InputSegmentFactory` 被调用 | 纯函数，无副作用；segmentId 格式固定、weekAnchor 计算仅依赖传入参数 | 读系统时间、访问 DB/网络 | T-B4-02 |

### 3.2 B3 不变量（不变，B4 不得破坏）

B3 蓝图 INV-B3-01~08 全部保持。B4 仅改变 segment 的**构造方式**（从 VM 内联 `InputSegment(...)` → 经 `InputSegmentFactory`），不改变 session/mapper/Runtime 合同。

### 3.3 AF-ARCH-02 边界检查表（ChatGPT 复核，B4 编码验收条件）

| # | 检查项 | B4 如何保证 | 验收 |
|---| ------ | ---------- | ---- |
| 1 | 每个 parser 构造时只传入一个 `InputSegment` | `StreamingMealSession.segmentParsers.getOrPut(seg.segmentId) { StreamingMealParser(segments = listOf(seg), ...) }` —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 2 | delta 只进入当前 segment 的 parser | `session.onDelta(seg.segmentId, event.text)` —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 3 | `finish_reason=length` 只标记当前 parser | 每段 `onCompleted` 只 finish 自己的 parser —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 4 | parser 内的残余 buffer 不跨 segment | 每段独立 parser 实例 —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 5 | snapshot 合并顺序与原 segments 顺序一致 | `segmentParsers` 是 `LinkedHashMap`，按 `orderedSegments` 的 `forEach` 遍历合并 —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 6 | 多次调用 snapshot 不会重复累计结果 | `snapshot()` 每次重新遍历 parser 列表构建新 `MealStreamDraft`，不缓存 —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 7 | `segmentId` 重复时有明确行为 | 本蓝图 §3.4 冻结 | 见下方 |

### 3.4 segmentId 唯一性不变量（B4 新增）

| ID | 条件 | 必须结果 | 证据 |
|----|------|---------|------|
| INV-B4-09 | 同一 `StreamingMealRequest` 内的 `segments` 列表 | 所有 `segmentId` 必须唯一 | T-B4-02 |
| INV-B4-10 | `InputSegmentFactory.createWeekSegments()` | 生成的 7 个 segmentId 互不相同（`week-{anchor}-day1` ~ `week-{anchor}-day7`） | T-B4-02 |
| INV-B4-11 | 快速记 segmentId 与周期记 segmentId | 格式不同（`quick-*` vs `week-*`），不会碰撞 | T-B4-03 |

> **建议**：在 `StreamingMealRequest` init 块加 `require(segments.map{it.segmentId}.distinct().size == segments.size) { "segmentId 重复" }` fail-fast。此为 🟡 建议（非阻断），若实施则需在 B3 session 测试中补"重复 segmentId 抛异常"用例。

---

## §4. 输入模型设计

### 4.1 InputMode（新增）

```kotlin
// 在 AiMealInputViewModel.kt 或 shared 中定义
enum class InputMode {
    /** 快速记：单输入框，最大 200 字 */
    QUICK,
    /** 周期记：按周/日期段，每天独立输入，最大 200 字/天 */
    WEEK,
}
```

放在 ViewModel 文件中（不跨模块使用），避免 shared 模块引入 UI 概念。

### 4.2 InputSegmentFactory（新建，shared）

> 设计来源：架构模型终审 S6（两位架构师独立一致结论）+ Google 架构工程师 B4 复审。

```kotlin
// shared/.../ai/meallog/InputSegmentFactory.kt

object InputSegmentFactory {

    /**
     * 快速记：单一 anchor 日期产生 1 个 segment。
     * segmentId = "quick-{targetDate}"
     */
    fun forQuickRecord(
        inputText: String,
        anchorDate: LocalDate,
    ): List<InputSegment> = listOf(
        InputSegment(
            segmentId = "quick-$anchorDate",
            targetDate = anchorDate,
            inputText = inputText.trim(),
            ordinal = 0,
        )
    )

    /**
     * 周期记：锚点日期所在周一为锚，产生该周 7 天（周一~周日）的 segments。
     * segmentId = "week-{weekAnchor}-day{1..7}"
     *
     * @param dayTexts 7 个字符串，index 0=Monday, ..., index 6=Sunday
     * @param weekAnchorDate 该周周一
     * @return 7 个 InputSegment（含空白段；由调用方通过 StreamingMealRequest.nonBlankSegments 过滤）
     */
    fun forPeriodicRecord(
        dayTexts: List<String>,
        weekAnchorDate: LocalDate,
    ): List<InputSegment> {
        require(dayTexts.size == 7) { "dayInputs must have exactly 7 elements" }
        return dayTexts.mapIndexed { index, text ->
            InputSegment(
                segmentId = "week-${weekAnchorDate}-day${index + 1}",
                targetDate = weekAnchorDate.plusDays(index),
                inputText = text.trim(),
                ordinal = index,
            )
        }
    }

    /**
     * 推算某日期所在周的周一（ISO 周一）。
     * 跨年周（如 2025-12-29 周一 → 周日 2026-01-04）正确处理。
     */
    fun mondayOfWeek(date: LocalDate): LocalDate {
        val dayOfWeek = date.dayOfWeek.ordinal  // 0=Mon, ..., 6=Sun
        return date.minusDays(dayOfWeek)
    }
}
```

**设计决策**：
- 快速记和周期记统一返回 `List<InputSegment>`（而非一个返回单个、一个返回列表）→ 调用方 `submit()` 的处理逻辑统一。
- 空白段保留在全量 7 段中 → `StreamingMealRequest.nonBlankSegments` 过滤（已有属性，不改）。
- `mondayOfWeek` 提取为 shared 公共函数 → 替代 ViewModel 私有的 `startOfWeek()`，消除双真相源风险（`weekAnchor` 与 `segmentId` 命名规则由同一工厂保证一致）。

**为什么下沉 shared**：
- 纯函数、无平台依赖 → 自然属于 commonMain
- `mondayOfWeek` 可被 shared 单测覆盖（跨年周、1月1日周一、12月31日所在周等边界）
- segmentId 命名规则 + weekAnchor 推算 + ordinal 排序在单一工厂内保证一致，消除当前 ViewModel 手动拼字符串的漂移风险
- 符合准则 B（共享逻辑不共享像素）

### 4.3 ViewModel 中的草稿管理

> 字段命名和交互行为由 Apple UX 设计师确定（2026-08-06）。

```kotlin
// AiMealInputUiState 新增/调整字段
data class AiMealInputUiState(
    // ... 既有字段不变 ...
    
    /** 当前输入模式（替代旧 InputMode.TEXT/VOICE，语音状态由 voiceState 独立表达） */
    val inputMode: InputMode = InputMode.QUICK,
    
    /** 快速记草稿（切换模式保留） */
    val quickDraftText: String = "",
    
    /** 周期记周锚点（所选周周一），默认当前日期所在周周一 */
    val periodWeekMonday: LocalDate = startOfWeek(today),
    
    /** 周期记选中日期范围（0..6 表示周一至周日），默认全选 7 天 */
    val periodSelectedRange: IntRange = 0..6,
    
    /** 周期记各天草稿（key=0..6=Mon..Sun，value=文本），仅保留有内容的天 */
    val periodInputs: Map<Int, String> = emptyMap(),
)
```

**设计决策（来自 UX 设计师）**：
- **`inputText` 保留但变更为计算属性**：快速记时 = `quickDraftText`，周期记时 = ""。`submit()` 内部按模式直接用 `quickDraftText` 或 `periodInputs`。
- **语音按钮仅在快速记模式显示**：周期记多输入框环境中语音指向不明确（需先点某输入框再长按语音 = 两步操作，不如直接打字）。周期记场景偏向"坐下来批量规划"而非"快速捕捉"。
- **标题文案跟随模式切换**："AI 快捷记一餐" vs "AI 周期记一餐"（双重编码：分段滑块+标题，消除迷惑）。
- **`InputMode` 枚举调整**：`QUICK, PERIOD`（`TEXT, VOICE` 语义已由 `voiceState` 覆盖）。

---

## §5. UI 设计（交互规范）

> 本节由 Apple UX 设计师产出（2026-08-06），包含精确布局 dp/sp、颜色、动效、适老化要求。编码时以此为唯一 UI 真相源。

### 5.1 快速记模式（基准态）

与 B3 基本一致，仅新增字符计数 + 发送按钮改用 CapsuleButton：

**改动点**：
- 输入框右下角内嵌字符计数 `"45 / 200"`（`bodySmall` 12sp），<180 灰色 → 180-199 琥珀 `#E0A23C` → ≥200 错误红 `#D14E3B` + 拒绝输入
- 粘贴超 200 字截断 + Snackbar "已截取前 200 字"
- 发送按钮从 Material `Button` 改为 **`CapsuleButton`**（全 App 主 CTA 统一，§9.10/§9.13）
- 语音按钮保留（仅快速记）

### 5.2 周期记模式（新增）

#### 5.2.1 模式切换

```
┌── ModalBottomSheet ──────────────────────────────┐
│ ✨ AI 周期记一餐                          (i)    │  ← 标题跟随模式
│                                                   │
│ ┌───────────────────────────────────────────────┐ │
│ │       快速记      │     周期记               │ │  ← SegmentedControl
│ └───────────────────────────────────────────────┘ │
│                                                   │
│ ┌── WeekStrip（7天日期段选择器·签名元素）──────┐  │
│ │  一    二    三    四    五    六    日       │  │
│ │  ██    ██    ██    ██    ██    ░░    ░░      │  │  ██=accent-container 赤陶橘
│ │   4     5     6     7     8     9    10      │  │  ░░=surfaceVariant 浅灰
│ └──────────────────────────────────────────────┘  │
│ 已选 5 天 · 周一至周五                            │
│                                                   │
│ ┌ 周一 8月4日 ─────────────────────────────────┐ │
│ │ ┌──────────────────────────────────────┐ 12/200│ │  ← PeriodDayBlock
│ │ │ 今天早餐吃得比较清淡…                  │       │ │  heightIn(100,160dp)
│ │ └──────────────────────────────────────┘       │ │
│ └───────────────────────────────────────────────┘ │
│ ... (已选天 × N，AnimatedVisibility 展开/收起)    │
│                                                   │
│ ┌───────────────────────────────────────────────┐ │
│ │           📤  发送（CapsuleButton）            │ │
│ └───────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────┘
```

#### 5.2.2 WeekStrip（新建组件·签名元素）

**文件**：`androidApp/.../ui/ai/WeekStrip.kt`

| 属性 | 取值 |
|------|------|
| 整体宽度 | `fillMaxWidth`，铺满 sheet 内容区 |
| 天格 | 7 格均分 `Modifier.weight(1f)`，高 `56dp`（≥触达底线 48dp + padding） |
| 天格间距 | `horizontalArrangement = Arrangement.spacedBy(0.dp)`，视觉间距由每格 `padding(horizontal = 1.5.dp)` 产生 |
| 选中天格 | `primaryContainer` 底色 + 文字 `onSurface`（深色模式自适配）+ 字重 `Semibold` |
| 未选天格 | `surfaceVariant` 底色 + 文字 `onSurfaceVariant` + `Normal` 字重 |
| 连续选中段 | 相邻选中格无缝连成 `primaryContainer` 色条（头尾 `RoundedCornerShape(10dp)`，中间格 `RoundedCornerShape(0)`） |
| 天格内部 | `Column`：上周几标签（`labelMedium` 13sp）+ 下日期数字（`bodyMedium` 15sp），居中 |
| 今天标识 | 日期数字始终 `primary` 字色 + `Semibold`（无论该格是否选中；全选态时今天底色与其他天一致，仍需要"我在哪"的定位锚） |
| 范围描述行 | 天格下方 4dp：`"已选 X 天"`（`labelSmall` 12sp, `onSurfaceVariant` 色，居中） |

**交互**（点击端点设置范围）：
- **默认**：全部 7 天选中（当前日期所在周的周一至周日）
- **点未选中天（相邻）**：扩展范围到该天
- **点未选中天（不连续）**：重置范围为仅该天
- **点选中段左端天**：左端点右移一天（取消最左）
- **点选中段右端天**：右端点左移一天（取消最右）
- **点选中段中间天**：无操作
- **仅剩 1 天时点它**：无操作（不可取消最后一天，最少 1 天）

**动效**：选中段背景 `animateColorAsState` 300ms `tween`；输入块出现/消失用 `AnimatedVisibility(expandVertically + fadeIn/fadeOut)` 200ms。

#### 5.2.3 PeriodDayBlock（新建组件）

**文件**：`androidApp/.../ui/ai/PeriodDayBlock.kt`

| 属性 | 取值 |
|------|------|
| 日期标签 | `titleSmall`（17sp Semibold），`label` 色，左对齐，格式"周一 8月4日" |
| 标签与输入框间距 | 8dp |
| 输入框 | `OutlinedTextField`，`heightIn(min = 100.dp, max = 160.dp)`，`RoundedCornerShape(12dp)` |
| 占位符 | "描述这一天的饮食…" |
| 字符计数 | 右下角内嵌 overlay（同快速记格式 `"XX / 200"` + 颜色分级） |
| 块间距 | 块与块之间 12dp（`Arrangement.spacedBy(12.dp)`） |

#### 5.2.4 发送按钮

- 组件：**`CapsuleButton`**（全 App 统一，替旧 Material `Button`）
- 文本：`"发送"` + `Send` 图标（18dp）
- `enabled`：周期记模式 `periodInputs.any { it.value.isNotBlank() }`；快速记 `quickDraftText.isNotBlank()`
- 文案：快速记 `"发送"`，周期记 `"发送 · N 天"`（N = 非空日期段数）
- 宽度：`fillMaxWidth`（底部主 CTA 标准）

### 5.3 复用组件清单

| 组件 | 来源 | 复用方式 | 改动 |
|------|------|---------|------|
| `SegmentedControl` | `component/SegmentedControl.kt` | 标题下插入，`options=["快速记","周期记"]`，`onSelect` 切 VM 模式 | 零改动 |
| `CapsuleButton` | `component/CapsuleButton.kt` | 替换底部发送 `Button` | 零改动 |
| `ModalBottomSheet` | M3 1.1.2 | 维持现有容器 | 零改动 |
| `OutlinedTextField` | M3 | 快速记 ×1、周期记 ×N | 仅调 `heightIn` 参数 |
| `Icons.Outlined.*` | Material Icons | 标题行、发送按钮图标 | 零改动 |
| `VoiceRecognizer` | `androidApp/ai/` | 仅在快速记模式保持 | 零改动 |
| 粘贴按钮 | 现有 `TextButton` overlay | 仅在快速记模式保留 | 零改动 |

### 5.4 新建组件清单

| 组件 | 文件 | 签名 |
|------|------|------|
| **`WeekStrip`** | `androidApp/.../ui/ai/WeekStrip.kt` | `fun WeekStrip(weekMonday: LocalDate, selectedRange: IntRange, onRangeChange: (IntRange) -> Unit, modifier: Modifier)` |
| **`PeriodDayBlock`** | `androidApp/.../ui/ai/PeriodDayBlock.kt` | `fun PeriodDayBlock(dateLabel: String, inputText: String, onTextChange: (String) -> Unit, modifier: Modifier)` |

> 字符计数逻辑不单独抽组件——通过 `Box` overlay 内联实现（2 行 Compose 代码）。

### 5.5 动效清单（克制）

| 元素 | 动效 | 时长 |
|------|------|------|
| SegmentedControl 滑块 | 现有内置滑动动效 | ~250ms |
| WeekStrip 范围变化 | 选中段 `animateColorAsState` 背景色 + 天格文字色平滑过渡 | 300ms `tween` |
| 输入块出现/消失 | `AnimatedVisibility` 垂直展开/收起（`expandVertically` + `fadeIn`/`fadeOut`） | 200ms |
| 字符计数变色 | `animateColorAsState` tertiary → warning → error | 200ms |
| 模式切换 | 输入区内容 `AnimatedVisibility` 淡入淡出 | 200ms |

### 5.6 适老化（Android fontScale 2.0 + 显示尺寸最大）

- WeekStrip 天格 ≥56dp（直接满足触达底线）
- 选中天格**三重编码**：颜色（赤陶橘实底）+ 字重（Semibold）+ 位置（左→右 Mon→Sun）+ 范围描述行文字确认
- 输入框 minHeight 随字号动态放大
- 字符计数位置不漂移、不截断
- 发送按钮 ≥56dp 高

### 5.7 设计决策记录

| 决策 | 理由 |
|------|------|
| WeekStrip 用均分 7 格而非横滚 | 7 格 × ~45dp = 315dp，在 360dp 宽 Sheet 内（扣除 40dp 边距剩 320dp）刚好铺满 |
| 连续选中用端点点击而非拖拽 | 拖拽在小格子上（~45dp）易误触；端点点击清晰简单，符合苹果"能点不拖" |
| 快速记保留语音、周期记移除 | 周期记多输入框语音指向不明确（需先点框再长按 = 两步操作） |
| 字符计数内嵌输入框右下角 | iOS 备忘录/信息/邮件均如此，不额外占行 |
| CapsuleButton 替旧 Button | 全 App 主 CTA 统一为胶囊按钮（§9.10/§9.13），趁机收敛不一致特例 |
| WeekStrip 圆角 10dp | SegmentedControl 轨道圆角同值，同为分段选中控件圆角一致 |
| 天格高度 56dp | 标准触达底线 48dp + padding，直接满足适老要求 |

## §6. 数据流（B4 变更部分）

```
用户选择模式（SegmentedControl）
  ↓ vm.setInputMode(QUICK/PERIOD)
UiState.inputMode 更新
  ↓
用户输入（快速记: quickDraftText / 周期记: periodInputs[dayIndex]）
  ↓ vm.setQuickDraft(text) / vm.setPeriodInput(dayIndex, text)
UiState 草稿更新（each mode independent）
  ↓ 用户点击发送
vm.submit()
  ├─ QUICK: InputSegmentFactory.forQuickRecord(quickDraftText, targetDate)
  │   → StreamingMealRequest(segments=..., ...)
  │
  └─ PERIOD: InputSegmentFactory.forPeriodicRecord(periodInputs, periodWeekMonday)
      → StreamingMealRequest(segments=..., ...)
  ↓
StreamingMealSession(request)
  ↓ 与 B3 完全一致的 while(nextSegment()) 循环
AiMealPrompt.buildStreamingRequest(seg)  // 单段（§1 AF-ARCH-03 收窄）
  ↓
AiRuntime.stream(llmRequest).collect { ... }
  ↓
handleSessionSnapshot → previewAll → AutoGenPreview
  ↓ 用户确认
commitPreview
```

**与 B3 的唯一差异**：`submit()` 开头 15 行的 segment 构造逻辑。其余全部不变。

---

## §7. 逐文件机械实施脚本

> **命名约定（§4 真相源）**：`InputMode.QUICK/PERIOD`、`quickDraftText`、`periodInputs: Map<Int,String>`、`periodWeekMonday`、`periodSelectedRange`。以下全部代码与 §4/§5 精确一致。

### 步骤 0：AF-ARCH-03 冻结（必须先做，独立 commit）

**文件**：`shared/.../ai/meallog/AiMealPrompt.kt:93-127`

1. 改 `buildStreamingRequest` 签名：
   ```kotlin
   // 旧
   fun buildStreamingRequest(segments: List<InputSegment>): LlmRequest
   // 新
   fun buildStreamingRequest(segment: InputSegment): LlmRequest
   ```
2. 删 `buildStreamingUserPrompt` 中 `else { ... }` 多段分支（第 117-125 行）+ 删 `maxTokens` 缩放逻辑（第 98-101 行），固定 `maxTokens = 2048`。
3. 更新唯一调用点 `AiMealInputViewModel.kt:306`：`buildStreamingRequest(listOf(seg))` → `buildStreamingRequest(seg)`。
4. 运行 `:shared:testDebugUnitTest` 确认 0 失败。

### 步骤 1：新建 InputSegmentFactory + 测试（shared）

**文件**：`shared/.../ai/meallog/InputSegmentFactory.kt`（新建）

按 §4.2 精确签名实现 `forQuickRecord` / `forPeriodicRecord` / `mondayOfWeek`。

**测试**：`shared/.../test/.../InputSegmentFactoryTest.kt`（新建）
- `forQuickRecord`：segmentId 格式、inputText trim、ordinal=0、isBlank
- `forPeriodicRecord`：7 段生成、segmentId 格式 `week-{anchor}-day1..7`、ordinal 0-6、空白段保留
- `mondayOfWeek`：正常周、跨年周（2025-12-29→周一 2025-12-29）、1月1日周一、12月31日所在周

### 步骤 2：扩展 ViewModel（接入点 1, 5, 6）

**文件**：`androidApp/.../ui/ai/AiMealInputViewModel.kt`

2.1 `AiMealInputUiState` 新增字段（与 §4.3 一致）：
```kotlin
val inputMode: InputMode = InputMode.QUICK,
val quickDraftText: String = "",
val periodWeekMonday: LocalDate = ...,  // 默认当前日期所在周周一
val periodSelectedRange: IntRange = 0..6,
val periodInputs: Map<Int, String> = emptyMap(),
```

2.2 新增 VM 动作（字段名与 §4.3 一致）：
```kotlin
fun setInputMode(mode: InputMode) { ... }        // 切换模式，保留对方草稿
fun setQuickDraft(text: String) { ... }           // take(200)
fun setPeriodInput(dayIndex: Int, text: String) { ... }  // take(200)，写 periodInputs
fun setWeekRange(start: Int, end: Int) { ... }    // 调整 periodSelectedRange
fun advanceWeek() { ... }                         // periodWeekMonday + 7，清空 periodInputs
fun retreatWeek() { ... }                         // periodWeekMonday - 7，清空 periodInputs
```

2.3 `submit()` 改造（`AiMealInputViewModel.kt:268-280`，接入点 1）：
```kotlin
fun submit() {
    val state = _state.value
    val segments = when (state.inputMode) {
        InputMode.QUICK -> {
            val text = state.quickDraftText.trim()
            if (text.isBlank()) return
            InputSegmentFactory.forQuickRecord(text, state.targetDate)
        }
        InputMode.WEEK -> {
            InputSegmentFactory.forPeriodicRecord(
                dayTexts = (0..6).map { state.periodInputs[it] ?: "" },
                weekAnchorDate = state.periodWeekMonday,
            )
        }
    }
    if (segments.all { it.isBlank }) return

    val generationId = "meal-${++generationCounter}"
    val request = StreamingMealRequest(
        segments = segments,
        generationId = generationId,
        weekAnchor = InputSegmentFactory.mondayOfWeek(
            segments.firstOrNull { !it.isBlank }?.targetDate ?: state.targetDate
        ),
    )
    // ... 后续与 B3 完全一致（session 创建、generation 清空、while 循环）...
}
```

2.4 **删除 `startOfWeek()` 私有方法**（`AiMealInputViewModel.kt:486-489`）→ 替换为 `InputSegmentFactory.mondayOfWeek()`。

2.5 `setInputText()` 兼容：快速记模式时调用 `setQuickDraft()`，周期记模式时不操作。

### 步骤 3：新建 UI 组件 + 扩展 InputPhase

**新建文件**：
- `androidApp/.../ui/ai/WeekStrip.kt`（§5.2.2 规格）
- `androidApp/.../ui/ai/PeriodDayBlock.kt`（§5.2.3 规格，原名 PeriodDayInputBlock）

**修改文件**：`androidApp/.../ui/ai/AiMealInputSheet.kt`

3.1 `InputPhase` 内部按 `state.inputMode` 分叉 → `QuickInputSection`（既有代码搬迁）/ `PeriodInputSection`（新增）

3.2 `PeriodInputSection` 使用 `WeekStrip` + `PeriodDayBlock` 列表 + `CapsuleButton`

3.3 发送按钮改为 `CapsuleButton`，文案：快速记 `"发送"` / 周期记 `"发送 · N 天"`

3.4 标题文案跟随模式："AI 快捷记一餐" / "AI 周期记一餐"

### 步骤 4：测试

见 §9。

### 步骤 5：构建与台账

```bash
scripts\build-cli.bat :shared:testDebugUnitTest
scripts\build-cli.bat :androidApp:testDebugUnitTest --tests "com.sxdbsm.cookbook.android.ui.ai.AiMealInputViewModelStreamTest"
scripts\build-cli.bat :androidApp:assembleDebug
```

### B4 最小改动集（架构工程师确认，命名已对齐 §4）

| # | 文件 | 改动 | 依赖 |
|---|------|------|------|
| 1 | **新建** `shared/.../meallog/InputSegmentFactory.kt` | `forQuickRecord` / `forPeriodicRecord` / `mondayOfWeek` + 单测 | 无 |
| 2 | `AiMealPrompt.kt:93-127` | `buildStreamingRequest` 签名收窄为单段，删多段分支+maxTokens缩放 | 无 |
| 3 | `AiMealInputViewModel.kt:268-280,486-489` | `submit()` 改用 `InputSegmentFactory` + 删 `startOfWeek()` + 新增 `InputMode`/草稿字段/方法 | 依赖 #1 |
| 4 | `AiMealInputViewModel.kt:306` | `buildStreamingRequest(listOf(seg))` → `buildStreamingRequest(seg)` | 依赖 #2 |
| 5 | **新建** `WeekStrip.kt` + `PeriodDayBlock.kt` | 周期记日期段选择器 + 每日输入块 | 依赖 #3 |
| 6 | `AiMealInputSheet.kt` InputPhase | 模式切换 + PeriodInputSection + CapsuleButton | 依赖 #3,#5 |

**不改**：`StreamingMealSession`、`StreamingMealParser`、`AiRuntime`、`MealStreamDraftMapper`、Repository、DI。

---

## §8. S1~S6 逐条显式处理（B3 蓝图 §11.3，ChatGPT 复核门禁 #2）

| # | 原建议 | B4 处理 | 理由 |
|---| ------ | ------- | ---- |
| **S1** | `PARTIAL_READY` 保存会取消后续段 | 🟡 **纳入 B5 蓝图**。B4 不改变保存逻辑（`PARTIAL_READY` 可保存，`generationJob?.cancel()` 取消剩余段）。B5 需定策略：`isTerminal` 前禁止保存，或保存前告知"仅保存已完成 N 天，其余 M 天将放弃"。B4 周期记的 segment 列表已含 ordinal 顺序，底层已就绪。 | B4 只做输入 UI，保存行为归 B5。B3 单段无此问题；B4 周期记可能暴露但非本批范围。 |
| **S2** | Delta 路径 preview 太频繁 | 🟡 **纳入 B5 蓝图**。B4 不改 `handleSessionSnapshot` 的调用时机。B5 应将 preview 触发改为"段终态 + final + 防抖"而非每 Delta。 | B4 不改变 B3 的生成/预览链路。周期记 7 段时此优化价值更大，但归 B5。 |
| **S3** | snapshot 结构需表达 per-day 失败信息 | 🟡 **纳入 B5 蓝图**。`StreamingSessionSnapshot` 目前已有 `segmentStates: Map<String, StreamSegmentState>`，B5 UI 可通过它展示"第 N 天失败"。如需更丰富信息（如 per-segment 诊断分组），B5 扩展 snapshot。 | 当前结构已支持基本的 per-segment 状态展示。B4 不改 snapshot。 |
| **S4** | `AiMealSessionPort` 签名需多天支持 | 🟡 **纳入 B5 蓝图**。`preview(days, targetDate)` 和 `commit(preview)` 当前支持多天（`days` 已是 `List<DayMealJson>`）。`parseRule(input, targetDate)` 仅单串——如 B5 需要按天降级，届时扩展。 | B4 不改 port。当前签名对多天场景足够。 |
| **S5** | `sessionPort` 改为构造参数注入 | 🟡 **降级到 B4 后技术债批次**。`replaceSessionPortForTest` 已存在且正常工作。改为构造参数注入属纯重构（零功能变更），但需改 ViewModel 构造器签名 + Koin DI + 全部测试构造点，对 B4 输入 UI 无实际依赖。B4 最小化改动面优先。 | 消除可变注入点的并发面价值认同，但 B4 不改 sessionPort 调用路径（仅 `submit()` 中 segment 构造逻辑变化），此重构与 B4 正交。 |
| **S6a** | 周起始日算法下沉 InputSegmentFactory | ✅ **已采纳**。`InputSegmentFactory` 含 `forQuickRecord` + `forPeriodicRecord` + `mondayOfWeek` 三个纯函数。ViewModel 的 `startOfWeek()` 删除，统一调用 `mondayOfWeek()`。周期记 segment 构造经 `forPeriodicRecord` 而非 ViewModel 内联。 | 架构工程师复审确认：segmentId 命名规则 + weekAnchor 推算 + ordinal 排序在单一工厂内保证一致。 |
| **S6b** | 健康摘要脱敏下沉 HealthContextSummarizer | 🟡 **纳入后续技术债批次**（不阻塞 B4/B5）。涉及"绝不发送姓名/ID"红线，但目前快速记/周期记的健康上下文来自 `buildHealthSafetyReport`（已在 VM 内），B4 不新增联网风险。 | B4 不改健康摘要。下沉可独立做、不绑定 B4。 |

---

## §9. 测试矩阵

### 9.1 Shared 测试（InputSegmentFactoryTest，新建）

| ID | 前置与刺激 | 精确断言 |
|----|----------|---------|
| T-B4-F1 | `forQuickRecord("  午饭  ", date)` | `segmentId = "quick-{date}"`, `inputText = "午饭"`, `ordinal = 0`, `isBlank = false`, 返回 `List` 含 1 元素 |
| T-B4-F2 | `forQuickRecord("   ", date)` | `isBlank = true`（空白段保留在列表中） |
| T-B4-F3 | `forPeriodicRecord(listOf("a","","c","","","",""), anchor)` | 返回 7 个 segment；segmentId 为 `week-{anchor}-day1..7`；ordinal 0..6；空白段 isBlank=true 但保留 |
| T-B4-F4 | `mondayOfWeek(2025-12-31)` | 返回 `2025-12-29`（跨年周周一） |
| T-B4-F5 | `mondayOfWeek(2026-01-01)` | 返回 `2025-12-29`（1月1日周四，所在周周一） |

### 9.2 ViewModel 测试（扩展 AiMealInputViewModelStreamTest）

| ID | 前置与刺激 | 精确断言 |
|----|----------|---------|
| **T-B4-01** | 初始 state；调用 `setInputMode(WEEK)` | `inputMode = WEEK`；`quickDraftText` 保持原值；`periodInputs` 为空 Map |
| **T-B4-02** | WEEK 模式，`periodInputs = mapOf(0 to "周一文本", 2 to "周三文本", 4 to "周五文本")`，其余天无 key；调用 `submit()` | 构造的 `StreamingMealRequest` 仅含 3 个 segment（空白被 `nonBlankSegments` 过滤）；segmentId 为 `week-{anchor}-day1`、`day3`、`day5`；ordinal 为 0, 2, 4；targetDate 分别为周一/三/五 |
| **T-B4-03** | QUICK 模式，`quickDraftText = "午饭"`；调用 `submit()` | `StreamingMealRequest` 含 1 个 segment；segmentId = `quick-{date}`；行为与 B3 完全一致 |
| **T-B4-04** | QUICK 模式 `setQuickDraft("午饭")` → `setInputMode(WEEK)` → `setInputMode(QUICK)` | `quickDraftText = "午饭"`（未丢失）；`inputMode = QUICK` |
| **T-B4-05a** | WEEK 模式 `setPeriodInput(0, 250字文本)` | `periodInputs[0]?.length = 200`（截断） |
| **T-B4-05b** | QUICK 模式 `setQuickDraft(250字文本)` | `quickDraftText.length = 200`（截断） |
| **T-B4-06** | WEEK 模式 `periodInputs` 全部空 → `submit()` | `submit()` 直接 return（`segments.all { it.isBlank }`），不创建 session |
| **T-B4-07** | WEEK 模式，`periodWeekMonday = 2025-12-29` | `forPeriodicRecord` 生成的 day7 = 2026-01-04（跨年周日），日期计算正确 |

### 9.3 B3 回归测试

B3 既有测试（T-B3-01~09 + session/mapper/parser/runtime）全部保持通过。

---

## §10. 审查裁决延后项（2026-08-06 三角色审查记录）

以下审查发现不阻塞 B4 交付，记录到对应批次：

| 来源 | 项目 | 裁决 | 目标批次 |
|------|------|------|---------|
| Apple #3 | advanceWeek/retreatWeek 未接通 UI | 🟡 B4 蓝图未要求周切换（WeekStrip 是点选范围），待 B5 接通 | B5 |
| Google质量 B4-S1 | periodSelectedRange 语义澄清 | ⚪ 可见范围≠提交范围，加注释说明 | B5 蓝图 |
| Google架构 S1 | mondayOfWeek 在 submit() 中冗余反推 | ⚪ 不阻塞，逻辑安全 | B6 收尾 |
| Google架构 S2 | 200 字上限散布 4 处，未提取常量 | 🟡 提取 `MAX_INPUT_CHARS` 公共常量 | B5/B6 |
| Google架构 S3 | InputSegment.charCount 死代码 | ⚪ 保留不删（1 行 getter，伤害低） | 不处理 |
| Apple #3 | VoiceRecognizer 泄漏（B3 预存） | ⚪ 非 B4 引入，记录 | 技术债 |
| Apple #4 | 两模式字符计数动画不一致 | ⚪ 抽 CharCountLabel 组件 | B5 |
| Apple #5 | 截断层不一致（VM 层 vs UI 层） | ⚪ 统一在 VM 层截断 | B5 |
| Apple #7 | 切周清空草稿无撤销 | 🟡 接通 UI 时加 Snackbar 撤销 | B5 |
| Apple #8 | 字符计数 overlay 可能压文字 | ⚪ 评估后加底部 padding | B5 视觉打磨 |

> **下次统一处理**（B5 蓝图起草时强制列入）：200 字常量提取、CharCountLabel 组件、截断层统一、周切换 UI 接通、撤销 Snackbar。

---

## §11. 交付与放行判定

### 11.1 放行条件

1. §0 的 4 项 ChatGPT 门禁全部勾销（§1 AF-ARCH-03 冻结 + §8 S1~S6 处理 + 测试确认 + §3.3 边界检查表）。
2. AF-ARCH-03 代码落地（`buildStreamingRequest` 签名收窄为单段 + 多段分支删除）并通过 shared 单测。
3. T-B4-F1~F3 全部通过（shared 新增）。
4. T-B4-01~07 全部通过（ViewModel 新增）。
5. 既有 B3 测试（T-B3-01~09 + session/mapper/parser/runtime）全部保持 0 失败。
6. 三条构建命令串行当次成功：
   - `scripts\build-cli.bat :shared:testDebugUnitTest`
   - `scripts\build-cli.bat :androidApp:testDebugUnitTest --tests "com.sxdbsm.cookbook.android.ui.ai.AiMealInputViewModelStreamTest"`
   - `scripts\build-cli.bat :androidApp:assembleDebug`
7. 本蓝图状态由 `DRAFT` → `ACCEPTED`。

### 11.2 B4 → B5 过渡条件

B4 编码完成后、B5 开始前：
- B4 全部放行条件满足
- 真机验证通过（快速记 + 周期记输入 UI）
- B5 蓝图起草（确认页流式展示）

---

## §12. 与 B3 蓝图的关系

B3 蓝图的 §11.6 放行判定要求本文（B4 蓝图）完成后将 B3 蓝图状态由 `BLOCKED` 改为 `ACCEPTED`。

**时序**：
1. B3 蓝图 §11.6 → 检查本文 §0 的 4 项门禁全部满足 → B3 蓝图 `BLOCKED` → `ACCEPTED`
2. 本文 §10.1 全部满足 → 本文 `DRAFT` → `ACCEPTED`
3. 开始 B4 编码

两个蓝图可以同时 `ACCEPTED`，但 B4 编码必须在 B3 蓝图 `ACCEPTED` + 本文 `ACCEPTED` 之后。

---

## §13. 真机待验证清单（B4 预登记）

| 编号 | 验证项 | 操作步骤 | 状态 |
|------| ------ | -------- | :--: |
| E-B4-01 | 快速记/周期记模式切换 | ①打开 AI 记一餐 Sheet → 看到默认"快速记"②点击"周期记"→ 输入区切换为 7 天列表③切回"快速记"→ 之前输入的草稿还在 | ⬜ |
| E-B4-02 | 周期记日期段导航 | ①周期记模式 → 点击左右箭头切换周②确认日期标签正确（周一~周日）③当前周高亮或默认选中 | ⬜ |
| E-B4-03 | 每日输入 + 200 字限制 | ①周期记模式下在周一输入框输入文字 → 字符计数实时更新②输入超过 200 字 → 自动截断，计数变红③粘贴超过 200 字内容 → 截断 + Toast 提示 | ⬜ |
| E-B4-04 | 周期记发送 → 流式生成 | ①在周一/周三/周五填内容，其余留空 → 点击发送②确认只发了 3 个请求（非 7 个）③生成过程中可看到进度（第 1/3 天...）④空白天不产生预览内容 | ⬜ |
| E-B4-05 | 草稿独立保留 | ①快速记输入"午饭"②切换到周期记③切换回快速记 → "午饭"还在④周期记周一写"早餐"⑤切换到快速记⑥切换回周期记 → 周一"早餐"还在 | ⬜ |
| E-B4-06 | 全部空白不可发送 | ①周期记模式全部 7 天空白 → 发送按钮灰色不可点击②快速记模式输入框空白 → 发送按钮灰色 | ⬜ |
