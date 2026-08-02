# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。维护约定：只保留当前状态·每次**全覆盖**。
> 更新时间：**2026-08-02 · AI记一餐解析Bug排查完成·待修复**

---

## 🎯 本次 Session 成果

| 事项 | 状态 |
|------|------|
| 拉取真机数据库（`GCL0220212004523`）→ `temp/claude/phone_cookbook.db` | ✅ |
| AI 记一餐解析全链路 Bug 排查 + 根因定位 | ✅ |
| **排查报告**：`.ai-context/docs/feature/20260802_AI记一餐解析全链路错误_排查报告.md` | ✅ |
| **修复方案**：`.claude/plans/velvet-finding-quill.md` | ✅ 已批准 |
| 编码自查铁律提炼 → `~/.ai-context/knowledge/coding_selfcheck.md` | ✅ |

---

## ⏭ 下一个 Session 做什么

### 立即执行：修复 3 个 Bug（方案已批准·未动代码）

| # | Bug | 文件 | 严重度 |
|---|-----|------|--------|
| **1** | 吃/喝动词未被剥离 → "吃了"成为菜名和食材 | `RuleMealParser.kt` | 🔴 阻断 |
| **2** | `splitSoft()` 单字分隔词"和""跟"被 `couldBeDish` 误杀 | `RuleMealParser.kt` | 🔴 阻断 |
| **3** | 语音 `VoiceRecognizer` 实例不匹配，松手永不停止 | `AiMealInputSheet.kt` | 🟡 建议 |

**详细修复方案** 在排查报告 §五，代码级方案在 `velvet-finding-quill.md`。

### 修复步骤

1. **读排查报告** → `.ai-context/docs/feature/20260802_AI记一餐解析全链路错误_排查报告.md`（全文·含全链路追踪）
2. **读修复方案** → `.claude/plans/velvet-finding-quill.md`
3. **修改代码**（按方案 §五）：
   - `shared/.../ai/meallog/RuleMealParser.kt`：+`removeEatingVerbs()` + 改 `splitSoft()`
   - `androidApp/.../ui/ai/AiMealInputSheet.kt`：重构语音实例管理
4. **加单测**：`"中午吃了红烧肉和米饭"` → 菜名"红烧肉""米饭"不含"吃了"；`"中午吃了土豆粉"` → 菜名"土豆粉"
5. **构建验证**：`:shared:testDebugUnitTest` + `:androidApp:assembleDebug`
6. **真机装包验证**：按排查报告 §七 逐条验证

---

## 📁 先读清单（新 session 第一件事）

1. **本文件**（已读）
2. **排查报告** `.ai-context/docs/feature/20260802_AI记一餐解析全链路错误_排查报告.md` ← **必读**
3. **修复方案** `.claude/plans/velvet-finding-quill.md`
4. `CLAUDE.md`（门禁/红线）
5. `shared/.../ai/meallog/RuleMealParser.kt`（Bug 1+2 所在地）
6. `androidApp/.../ui/ai/AiMealInputSheet.kt`（Bug 3 所在地）

---

## 🔴 关键调试信息

- **真机序列号**：`GCL0220212004523`（TAS-AN00 / HUAWEI）
- **包名**：`com.sxdbsm.cookbook.android`
- **DB 路径**：`/sdcard/Android/data/com.sxdbsm.cookbook.android/files/cookbook/db/cookbook.db`
- **拉 DB 命令**：`export MSYS_NO_PATHCONV=1; adb pull /sdcard/Android/data/com.sxdbsm.cookbook.android/files/cookbook/db/cookbook.db temp/claude/phone_cookbook.db`
- **脏数据**：ingredient 1207("吃了"), 1208("肉和米饭"), dish 774("吃了红烧肉和米饭"), dish 776("吃了土豆粉")

---

## 🔗 依赖关系

```
本次排查 ✅
    ↓
修复 Bug 1+2+3（RuleMealParser + AiMealInputSheet）
    ↓
构建 + 单测 + 真机验证
    ↓
合并 → 继续 P2-1 K1a/K1c 剩余工作
```
