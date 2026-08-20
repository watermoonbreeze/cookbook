# ARCH Review — `e927442`

## 1. 审核对象

- Commit: `e9274424b089716cec38c805ee5a140a7066d890`
- 上一 ARCH 状态：UEN `REWORK_REQUIRED`，已知 `AF-UEN-01/02`
- 本次审核角色：独立 ARCH
- 结论：**UEN 仍为 REWORK_REQUIRED，但原因发生变化：01/02 的代码修复可接受；新增 03 属于旧 ARCH 蓝图本身的设计缺陷，不记为 coder 偏离。**

## 2. AF-UEN-01 — RESOLVED_BY_CODE（待总回归）

### 原问题
`SINGLE_DAY + MANUAL` 内嵌 `AddDayFoodScreen` 时底部保存 CTA 被 `embedded` 条件隐藏，用户无法保存。

### `e927442` 修复
`AddDayFoodScreen` 的 `bottomBar = FormBottomBar(...)` 已不再被 `if (!embedded)` 包住。

### ARCH 判定
- 静态实现方向：**通过**
- 不在本轮再次重写保存链。
- B 日期重构会触碰同一文件，因此必须把“embedded 仍有保存 CTA”作为跨批冻结不变量重新验证。

## 3. AF-UEN-02 — RESOLVED_BY_CODE（待总回归）

### 原问题
AI 保存进入 `DONE` 后统一页不离开、无反馈，并因 `DONE != INPUT` 被误判 dirty。

### `e927442` 修复
Single AI / Period AI 的 `AiMealBody` 均传入 `onSaved`：
- `DONE` → 全局 Snackbar “已保存”
- `aiVm.reset()`
- `nav.popBackStack()`

并新增 `shouldLeaveAfterAiSave()` + 单测。

### ARCH 判定
- 静态实现方向：**通过**
- 总交付仍须验证 Single/Period AI 两条真实保存链都正确离开且无假性未保存提示。

## 4. AF-UEN-03 — NEW / ARCH_SPEC_DEFECT（BP/ARCH，不是 DEV）

### 现象
当前统一页在 AI 非 `INPUT` 时切换 Range 或 Method，会弹：

- “放弃当前预览？”
- 确认后调用 `aiVm.cancelGeneration()`
- 再执行 selector 变更

这使“普通 selector 切换”承担了 destructive discard 的副作用。

### 关键归因
**这不是 coder 擅自实现错误。旧 canonical UEN 蓝图 §6、INV-UEN-04、T-UEN-04 本身就明确要求这种行为。**

因此 defect 分类必须写为：

```text
AF-UEN-03
class = ARCH_SPEC_DEFECT / BP
coder_negative = false
```

不得在 coder 能力评估里记成 Luna 违反蓝图。

### 为什么旧设计需要修订
统一入口的 Range / Method 是同一页面的两个状态维度，不应把稳定的 AI Preview 当成“切换 selector 即丢弃”的一次性对象。真正的 discard 行为应发生在显式离页/关闭确认，而不是普通维度切换。

同时，当前 AI VM 是共享实例，只有一个全局 `phase`，所以不能简单允许 `PREVIEW_READY` 时从 Single Range 改到 Period Range：这会造成“页面已显示 Period，但 preview 仍属于 Single”的语义错配。

### 新裁决
采用**非破坏性 + 相容性锁定**：

| AI phase | Range selector | Method selector | destructive action |
|---|---|---|---|
| `INPUT` | 可切 | 可切 | 无 |
| `GENERATING` | 禁用 | 禁用 | 无 |
| `PARTIAL_READY` | 禁用 | 禁用 | 无 |
| `PREVIEW_READY` | 禁用 | 可在 AI↔Manual 间切 | 无；隐藏 AI preview 但 VM 保留 |
| `ERROR` | 禁用 | 可在 AI↔Manual 间切 | 无 |
| `SAVING` | 禁用 | 禁用 | 无 |
| `DONE` | 不作为可交互稳态；AF-UEN-02 同帧离页 | 同左 | reset + pop 属保存完成链 |

当 `PREVIEW_READY`：
`Single + AI → Single + Manual → Single + AI` 必须恢复同一个 AI Preview；不得弹放弃框，不得 `cancelGeneration()`。

当 AI 非 `INPUT` 被隐藏在 Manual 下时，**Range 仍锁定**，防止回到 AI 时 inputMode/preview 与 page range 错配。

### 保留的 destructive 路径
- 用户真正 Back/离开 Unified 页且 dirty：保留 `rememberUnsavedGuard`；确认离开后允许 `cancelGeneration()+popBackStack()`。
- AI 原独立 Sheet 的显式关闭/放弃契约不在本项修改范围。

## 5. 当前 ARCH 总结

本批不在 `e927442` 处 ACCEPT，原因不是 01/02 未修，而是：

1. 需要把 `AF-UEN-03` 作为旧蓝图修订正式落库并实现；
2. 用户新增 `DATE-CALENDAR-01`；
3. 既有 `HOME-MERGE-01` 尚待执行；
4. 用户要求三项一次完成后再总审核。
