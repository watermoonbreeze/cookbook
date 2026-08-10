# Phase 2C → 2D Handoff

> 本文件是 **Phase 2D 执行的强制入口**。Phase 2C 已 ACCEPT / CLOSED（`ced5f13f`）。
> 它回答：Phase 2D 开始前，哪些 Project Graph 决策已经冻结，以及 Verification Bootstrap 必须遵守什么边界。
> 它**不是**真机验证清单本身。
> Truth Source Priority 见实施蓝图 §22；Phase 2D 正式蓝图需进一步细化此处预告的原则。

---

## 1. Phase 2D 唯一职责

```text
Phase 2D
=
Verification Bootstrap
```

包括：

```text
最新唯一验证清单
→
Stable Verification entities

Verification
→
WorkItem mapping

Verification status reconciliation

required closure preparation
```

## 2. Phase 2D 不负责

```text
CurrentWork final reconcile   → Phase 2E
L3 Feature split decision     → Phase 2E
Legacy Views replacement      → Phase 3
Graph active                  → Phase 3
Observed Store                → Phase 4
Lifecycle CLI                 → Phase 4
CI Guard                      → Phase 5
```

## 3. Truth Source Priority（冻结）

Verification Stable ID 和具体语义，以：

```text
latest unique real-device verification checklist
```

为最高 Truth Source。

状态辅助可读取：

```text
SESSION_交接.md
BLUEPRINT_STATE.md
```

但：

> 不能用 SESSION 的简称重新定义 Verification ID。

## 4. Stable Verification ID 绝对规则（不可变）

```text
Verification Stable ID semantics are immutable.
```

例如 `E-K1I-02` 已经定义为某个设备验证项，禁止未来再重新解释成 `build pass` 或其他语义。

- 已有 Verification ID：如果语义与源清单一致 → 使用。
- 如果 Graph 当前同 ID 语义错误 → **修语义，不换 ID**。
- 禁止「旧 ID 留着 + 再创建新 ID 表示正确语义」导致双份。

## 5. 统计必须重算

历史参考值（如约 97 pending / 17 passed）只能作为历史参考，**禁止硬编码**。

Phase 2D 必须：

> 从执行时的最新唯一验证清单重新统计。

禁止：

> 目标必须迁 97 pending。

## 6. Verification Status 映射预告（基础原则）

```text
✅                  → pass
❌ 明确失败          → fail
明确因条件不能验证   → blocked
没有真实结果        → pending
```

UI 标记（⬜ / 🔧 / 🆕 等）可能只是新增/修改/待执行/分类标记，**不能机械映射为 pass/fail**；实际验证结果为空 → `pending`。

## 7. required 默认规则（沿用 Frozen Contract）

```text
Verification.required
default = true
```

仅明确是 optional 才可 `required: false`。禁止为绕过 Closure 把 required 设 false。

## 8. WorkItem Mapping（No Guess）

每个 Verification 必须对应真实 WorkItem。不能可靠确定时记录：

```text
VERIFY_UNMAPPED
```

不得猜。禁止：

```text
不知道属于谁 → F-TOOLS
不知道 WorkItem → 当前最接近标题的 K*
```

## 9. 不得为 Verification 随意新建 WorkItem

只有「验证清单明确代表一个真实、当前 WorkItem，且 Phase 2B 确实遗漏」才允许考虑补 WorkItem，且必须记录：

```text
WORKITEM_MISSING_FOR_VERIFY
```

并按照 Phase 2B Stable-ID 规则执行。不确定 → 不创建。

## 10. Verification Closure（再次冻结）

```text
WorkItem status: done
→
all required Verification = pass / not_required
```

任何 `required=true` 且 `pending` / `fail` / `blocked` 存在时：

```text
WorkItem cannot be done
```

**Plan status 不参与 Verification Closure**：`Plan completed` 不能抵消 `required Verification pending`。

## 11. Build/Test 仍不是 Phase 2D Verification

```text
Gradle build success
unit test pass
pg check pass
```

不能因为 Phase 2D 正在迁 Verification 就变成 Verification Entity；它们继续是 **Observed Fact**。

Phase 2D **不实现 Observed Store**：禁止 `observed.yaml` / `execution_history.yaml` / command verification nodes。

## 12. PoC Verification 先 reconcile 再迁新

现有 `F-AI-MEAL` 已有若干 PoC Verification。Phase 2D 必须：

```text
先 reconcile 已存在
再迁新的
```

禁止从清单全量写一遍造成重复 ID。

点名（已有稳定设备验证语义，Phase 2D 必须从真实最新清单重新核对 status/required/reason，但**不得改变 Identity**）：

```text
E-K1I-01
E-K1I-02
```

## 13. Source Coverage 与 disposition

正式 Phase 2D 蓝图必须建立 `PHASE2D_SOURCE_COVERAGE.md`，原则类似 Phase 2B：每一个 actionable verification row 必须有唯一 disposition。

预先冻结允许的 disposition 集合（正式蓝图再给完整定义）：

```text
KEEP_EXISTING_VERIFY
MIGRATE_VERIFY
UPDATE_EXISTING_VERIFY
SKIP_NON_VERIFICATION_ROW
DEFER_VERIFY_UNMAPPED
```

Handoff 中**禁止写死迁移数量**：`recompute at Phase 2D execution time`。

## 14. Graph mode 与边界

```text
mode: draft（整个 Phase 2D 保持）
Graph activation → Phase 3（Phase 2D 完成也不自动 active）
Schema / Validator / 生产代码：禁止修改
```

---

## 15. 交接基线

```text
Phase 2C:
ACCEPT / CLOSED

Architecture Review Commit:
ced5f13f1a90b71faf9e7fe0646af617307d4215

Known Blocking Issues:
0

Open Non-Blocking:
L3 FEATURE_SPLIT_CANDIDATE → Phase 2E
```

---

*Phase 2C → 2D Handoff · 2026-08-11。*
