# UEN-FINAL Repair Blueprint — AF-UEN-03

## 0. Supersession

本文件是旧 canonical：
`.ai-context/docs/feature/统一添加入口+悬浮导航栏_实施蓝图.md`
的 **repair overlay**。

发生冲突时，本文件仅对以下旧条款进行 supersede：
- 旧 §6 中“AI 非 INPUT 切 Range/Method 弹放弃确认并 cancel”的规则；
- 旧 `INV-UEN-04` / `T-UEN-04`；
- 旧 §23 中与 selector destructive switch 相关的限制。

其他 UEN 设计全部保留。

## 1. Defect record

### AF-UEN-03
- Severity: blocking UX/spec inconsistency
- Class: `ARCH_SPEC_DEFECT / BP`
- Coder negative: **NO**
- Base where observed: `e9274424b089716cec38c805ee5a140a7066d890`
- Cause: 旧 ARCH 蓝图把普通 selector 切换设计成 destructive discard。
- Repair principle: **selector 不丢稳定状态；不相容的 Range 通过锁定避免语义错配。**

## 2. Verified facts

当前 `UnifiedAddMealScreen`：
- shared `AiMealInputViewModel`
- `pageState` 只有 range/method
- `requestChange()` 在 AI `phase != INPUT` 时弹 `pendingChange` Dialog
- confirm 调 `aiVm.cancelGeneration()`
- Range 切换会调用 `aiVm.setInputMode(QUICK/WEEK)`

当前 VM：
- 单一全局 phase；
- Quick / Week 输入草稿均在同一 VM；
- `setInputMode()` 本身保留另一侧 draft；
- Preview rendering 由全局 phase 驱动。

因此“PREVIEW_READY 时自由换 Range”不安全，“切 Method 隐藏/恢复同一 Range 的 preview”安全。

## 3. New phase policy

新增纯函数（命名可等价，但语义必须唯一）：

```kotlin
internal fun canChangeMealRange(phase: AiMealPhase): Boolean =
    phase == AiMealPhase.INPUT

internal fun canChangeMealMethod(phase: AiMealPhase): Boolean =
    phase == AiMealPhase.INPUT ||
    phase == AiMealPhase.PREVIEW_READY ||
    phase == AiMealPhase.ERROR
```

`DONE` 不参与稳态 selector：AF-UEN-02 会立即离页。

### 状态表

| phase | Range | Method | 说明 |
|---|---|---|---|
| INPUT | enabled | enabled | 草稿跨 mode 保留 |
| GENERATING | disabled | disabled | 避免隐藏进行中的生成 |
| PARTIAL_READY | disabled | disabled | 流式中间态同上 |
| PREVIEW_READY | disabled | enabled | 可 AI↔Manual 隐藏/恢复 preview；Range 锁住 session identity |
| ERROR | disabled | enabled | 可临时去 Manual；返回 AI 保留 error/retry context |
| SAVING | disabled | disabled | commit in progress |
| DONE | n/a | n/a | 同帧离页 |

## 4. UI implementation

### STEP-UEN-F-1 — SegmentedControl 支持 enabled
文件：
`androidApp/.../ui/component/SegmentedControl.kt`

增加向后兼容参数：

```kotlin
enabled: Boolean = true
```

要求：
- 默认 true，其他调用点零行为变化；
- `clickable(enabled = enabled, ...)`；
- disabled 有明显但克制的视觉弱化；
- selected 状态仍可识别；
- 触摸/语义不伪装成可点击。

禁止：
- 为 UEN 单独复制 SegmentedControl；
- 改所有既有 caller。

### STEP-UEN-F-2 — 删除 destructive selector dialog
文件：
`UnifiedAddMealScreen.kt`

删除：
- `pendingChange`
- selector 专用 `AlertDialog`
- selector 路径的 `aiVm.cancelGeneration()`
- `requestChange()` 的“非 INPUT → pendingChange”逻辑

保留：
- Back 的 `rememberUnsavedGuard`
- Back 确认后的 `cancelGeneration()+popBackStack()`
- AF-UEN-02 的 onSaved reset+pop

### STEP-UEN-F-3 — 两个 selector 使用 phase policy

Range：
```text
enabled = canChangeMealRange(aiState.phase)
```

当 enabled 且用户选择：
- reduce `SelectRange`
- 若当前 method==AI，调用 `setInputMode(QUICK/WEEK)`
- 无 dialog / cancel / reset

Method：
```text
enabled = canChangeMealMethod(aiState.phase)
```

当 enabled：
- reduce `SelectMethod`
- 切回 AI 时，根据**当前锁定 range**调用 `setInputMode`
- 无 dialog / cancel / reset

关键：
当 PREVIEW_READY 从 AI→Manual 后，phase 仍 PREVIEW_READY，所以 Range 必须继续 disabled；这样 Manual 下不能偷偷改 Range，再返回 AI 造成 preview/range mismatch。

### STEP-UEN-F-4 — Invariants / tests

#### INV-UEN-17（正式保留）
AI `DONE` 后必须完成离页反馈闭环，不停留 DONE，不出现“保存后仍未保存”提示。

#### INV-UEN-18（新增）
`PREVIEW_READY` 同一 Range 内 `AI → MANUAL → AI`：
- 不弹放弃确认；
- 不调用 cancel/reset；
- Range 不变；
- 返回 AI 后仍是原 preview state。

#### INV-UEN-19（新增）
`phase != INPUT` 且 Range 与当前 AI session 绑定时，Range 不允许改变；防止 preview/inputMode 与 page range 错配。

#### INV-UEN-20（新增）
`GENERATING/PARTIAL_READY/SAVING` 时 selector 不允许触发维度变更；这属于暂时锁定，不是 destructive discard。

### 建议纯函数测试
在 `UnifiedAddMealStateTest.kt`：

- `inputAllowsRangeAndMethodChange`
- `previewLocksRangeButAllowsMethodChange`
- `generatingAndPartialLockBothSelectors`
- `savingLocksBothSelectors`
- `errorLocksRangeButAllowsMethodChange`
- 保留 `aiDoneRequiresLeavingUnifiedEntry`

如可低成本测试 `SegmentedControl.enabled`，可加；否则代码审查+真机。

## 5. User-visible evidence

追加：
- **E-UEN-17**：Single+AI 生成到 Preview → 切 Manual → 切 AI；无放弃框，原 Preview 恢复。
- **E-UEN-18**：Period+AI Preview → 切 Manual → AI；同上。
- **E-UEN-19**：Preview 时尝试切 Single/Period，Range 控件明确 disabled，range 不变。
- **E-UEN-20**：Generating/Saving 时两个 selector 均不可切。
- **E-UEN-21**：Back 离开 dirty Unified 仍有原 UnsavedGuard；确认离开后可 cancel/pop。

同时回归：
- E-UEN-01：Single Manual 可保存（AF01）
- E-UEN-16：AI 保存后正确离开且无假 dirty（AF02）

## 6. Acceptance

不得出现：
- selector 普通切换调用 `cancelGeneration()`
- “放弃当前预览？”由 Range/Method selector 触发
- PREVIEW_READY 允许 Range 改变
- PREVIEW_READY AI→Manual→AI 后 Preview 消失
- 为解决状态问题新建第二个 AiMealInputViewModel
