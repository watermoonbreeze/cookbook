# AI 快捷记一餐进阶 架构方案

> 配套验收合同：`AI快捷记一餐_进阶_验收合同.md`。建立在 `自动化基础能力层_架构方案.md` 之上。AI 精简体。

## 0. 元信息 🔴

| 字段 | 内容 |
|---|---|
| 版本 / 日期 / 作者 | v1.0 · 2026-08-01 · Opus(旗舰)出方案 |
| 任务级别 | 深度 |
| 交互模式 | 常规 |
| 依赖前置 | **强依赖** `自动化基础能力层`（preview/commit·营养估算）；**K1b(健康评价)强依赖 L1 合规闸门**（云端 AI 发脱敏数据前的不可绕过弹窗） |
| 被依赖 | L3 全 App 自动化·对话式记餐 |
| 关联验收合同 | `AI快捷记一餐_进阶_验收合同.md` |
| 关联待测试验证 | 时间戳最新的 `真机待验证清单_<yyyyMMddHHmm>.md` 中 K1-V1..V5 |
| 状态 | 📄待拍板 |

## 1. 目标与范围 🔴

**一句话价值**：把"AI 快捷记一餐"从"能入库的骨架"做成"**记之前先看到每餐营养/热量 + 家庭健康评价，确认再入库**"的完整闭环。

**In Scope**
- 接 Plan 1：解析→**preview(带营养/热量)**→确认→commit（补上现在缺的预览阶段）。
- K1a：预览确认页展示每菜/每餐营养素+热量（复用 `DayMealCardView`/`DishNutritionLine`）。
- K1c：规则引擎 weekday→date_offset 推算（`TextSegmenter.weekdayToIso` 落地）。
- K1b：AI 带家庭健康档案**脱敏摘要**→`health_evaluation` 输出→预览页评价卡（**L1 闸门后启用**）。
- K1f 已并入 Plan 1（别名归一），本方案不重复。

**Out of Scope**
- 自动生成能力本身（Plan 1 负责）。
- K1d 双端 Schema 标准化、K1e 全 App AI 结构转换层（独立待办·本方案仅在 K1b prompt 复用 K1e 紧凑结构，不整体做）。
- Phase3-5（多天预览页/周计划入口/编辑器 AI 补全/对话式）→ §7 roadmap。

## 2. 现状与差距 🔴

| # | 现状 | 证据 | 解法 |
|---|---|---|---|
| D1 | 两个 recorder **直接写库·无预览阶段**→无法"记前看营养" | `AiMealRecorder.record()` `shared/.../ai/meallog/AiMealRecorder.kt:47-109`、`MultiDayRecorder.recordAll()` 直接 saveDayMeals | 接 Plan 1 preview/commit·插预览态 |
| D2 | 预览页**不展示营养/热量**（K1a 未做） | Android `ui/ai/AiMealInputSheet.kt` 无营养行 | 消费 `AutoGenPreview.estimatedKcal`+复用 DishNutritionLine |
| D3 | 记的菜**热量恒 0**（同 Plan 1 D1 根因） | `AiMealRecorder.kt:212-248` 无营养估算 | Plan 1 能力层解决 |
| D4 | weekday 偏移**未实现** | `TextSegmenter.weekdayToIso()` 预留接口未落地 | §3 K1c |
| D5 | AI **不带家庭健康档案**·无逐成员评价 | `AiMealPrompt.kt` 无 health context·`UnifiedMealSchema` 无 health_evaluation | §3 K1b（L1 后） |
| D6 | 单/多天两套 recorder 重复 | `AiMealRecorder` vs `MultiDayRecorder` 同逻辑 | Plan 1 P3 统一为适配器 |

## 3. 架构设计 🔴

**分层**：解析层(现有 `AiMealParser`/`RuleMealParser`) → **能力层(Plan 1 `DayAutoGenerator`)** → UI 编排(VM)。本方案主要在**能力层接入**+**prompt/schema 扩展**+**UI 预览增强**。

**类职责表**

| 类/文件 | 职责 | 新建/改 |
|---|---|---|
| `AiMealInputViewModel.kt` | 解析→`DayAutoGenerator.preview`→存 preview 态→确认调 commit | 改 |
| `AiMealInputSheet.kt` | 预览态渲染营养/热量(K1a)+健康评价卡(K1b) | 改 |
| `TextSegmenter.kt` | `weekdayToIso`/weekday→date_offset 推算(K1c) | 改 |
| `AiMealPrompt.kt` | 加家庭健康脱敏摘要段(K1b) | 改 |
| `UnifiedMealSchema.kt` | 加 `health_evaluation` 字段(K1b·可空·向后兼容) | 改 |
| `HealthContextBuilder.kt` | `buildHealthContext(familyMembers)`→脱敏摘要串(K1b) | 新建 |
| `AiMealRecorder.kt`/`MultiDayRecorder.kt` | 降为 Plan 1 适配器(Plan1 P3) | 改 |

**数据流图**

```
用户输入(文字/语音) → AiMealParser/RuleMealParser → 解析结果(含 date_offset·weekday 由 K1c 推算)
   │  （K1b·L1后）prompt 注入 HealthContextBuilder 脱敏摘要 → AI 回 health_evaluation
   ▼  映射 Semantic*（Plan 1 中立结构）
DayAutoGenerator.preview(days, today, ctx)   ← Plan 1
   ▼
AutoGenPreview（每菜营养估算/每餐热量/新建清单）+ health_evaluation
   ▼  AiMealInputSheet 预览态
   ├─ 复用 DayMealCardView/DishNutritionLine 显营养热量(K1a)
   ├─ 健康评价卡：逐成员/逐餐/总体(K1b·免责)
   └─ 新建食材"新"标 + "将新建N种食材"(Plan 1 careFlag/清单)
   ▼  用户确认
DayAutoGenerator.commit(preview, mergeMode) → AutoGenResult → Snackbar
```

**关键决策**

| 决策 | 选择 | 理由 |
|---|---|---|
| 营养从哪来 | 全部走 Plan 1 能力层估算·**不让 AI 出营养数值** | 免责红线·AI 营养不采信(记忆 cloud-ai-validate-local-rules) |
| 健康评价定位 | AI 只出**文字评价**(逐成员/餐/总体)·不出数值判定·不点病名恐吓 | 健康免责红线·AI 健康评价"仅供参考·非医嘱" |
| K1b 门禁 | 强依赖 L1·L1 未落地则**K1b 不启用**(灰置+提示"开启云端AI需先同意数据告知") | 合规闸门·发脱敏数据给第三方须先不可绕过同意 |
| 脱敏范围 | 只传人数/年龄段/身高体重/慢病病种/生命阶段/膳食限制·**不传姓名/具体体检数值** | 隐私·K1b 红线 |
| 分期 | P2-1(本地·免L1) 先交付·P2-2(K1b·待L1) 后接 | 不被 L1 阻塞主链 |

## 4. 接口契约 🔴

**TextSegmenter（K1c）**
```kotlin
// weekday(1=周一..7=周日) + today → date_offset(取"最近的过去该星期几"·如今周四说"周三吃了"→ -1)
fun weekdayToDateOffset(weekday: Int, today: LocalDate): Int
// "周三"/"上周五"/"礼拜天" → weekday?；解析不出返 null
fun parseWeekdayHint(text: String): Int?
```

**HealthContextBuilder（K1b·shared）**
```kotlin
object HealthContextBuilder {
    // 脱敏摘要：成员数/各成员(年龄段·非精确年龄, 身高体重可选, 慢病病种, 生命阶段, 膳食限制)
    // 输出紧凑串(复用 K1e 短key思路省 token)·不含姓名/体检数值
    fun buildHealthContext(members: List<FamilyMemberHealth>): String
}
```

**UnifiedMealSchema（K1b·加字段·向后兼容）**
```kotlin
@Serializable data class HealthEvaluation(
    val perMember: List<MemberEval> = emptyList(), // {memberRef, note} note守免责
    val perMeal: List<MealEval> = emptyList(),
    val overall: String = "",
)
// 挂到解析结果根：val health_evaluation: HealthEvaluation? = null（可空·老响应不带不报错）
```

**AiMealInputViewModel（改·两阶段）**
```kotlin
// 解析后不直接入库，先产 preview 态
suspend fun parseAndPreview(input: String)   // → UiState.preview: AutoGenPreview + healthEval
fun confirmSave()                             // → DayAutoGenerator.commit → AutoGenResult
```

**错误/边界**：AI 未返 health_evaluation→评价卡不显(不报错)；营养全缺→显"营养待完善"非"约0"(Plan 1 保证)；weekday 解析不出→回退 date_offset 现有逻辑；L1 未同意→K1b 灰置。

**复用**：`DayMealCardView`/`DishNutritionLine`(K1a 零改复用)、Plan 1 `DayAutoGenerator`、现有 `AiMealParser`/`RuleMealParser`/`SchemaValidator`。

## 5. 数据模型 / DB

- **无表结构改动**（health_evaluation 仅解析态·不落库；如需持久化评价另议·MVP 不落）。
- `UnifiedMealSchema` 加可空字段=纯 Kotlin·无 DB 迁移。

## 6. 文件改动清单 🔴

**新建**：`HealthContextBuilder.kt`(shared/ai/meallog)、`HealthContextBuilderTest.kt`、`TextSegmenterTest`(补 weekday 用例)。

**修改**：`AiMealInputViewModel.kt`、`AiMealInputSheet.kt`、`TextSegmenter.kt`、`AiMealPrompt.kt`、`UnifiedMealSchema.kt`、`AiMealRecorder.kt`/`MultiDayRecorder.kt`(适配器·随 Plan 1 P3)。

**同步维护**：`功能路径索引.md`、时间戳最新的 `真机待验证清单_<yyyyMMddHHmm>.md` 中 K1-V1..V5。

## 7. 分阶段实施 🔴

| Phase | 内容 | 依赖 | DoD |
|---|---|---|---|
| P2-1 | 接 Plan1 preview/commit·K1a 预览营养热量·K1c weekday | Plan1 完成 | 预览页每菜显热量>0·"周三吃了"入对日期·单测绿 |
| P2-2 | K1b 健康评价(HealthContext+schema+评价卡) | **L1 合规闸门** | 配 Key+同意后·预览显逐成员评价(免责)·未同意灰置 |
| P2-3(roadmap) | 多天预览页/周计划入口/编辑器AI补全/对话式 | — | 后续单开 |

## 8. 红线与风险 🔴

| 红线/风险 | 规避 |
|---|---|
| AI 营养不采信 | 营养全走 Plan 1 本地估算·AI 不出数值 |
| 健康免责 | 评价"仅供参考·非医嘱"·不点病名·不承诺疗效·copywriter 定调 |
| 隐私(脱敏) | 只传病种/年龄段·不传姓名/体检值·L1 不可绕过同意后才发 |
| L1 未落地 | K1b 灰置+引导·P2-1 不含 K1b 可独立交付 |
| 改B表不刷新 Flow | eaten_ratio 回填复用 Plan 1(已并 observeEatenRatioChanges 令牌) |
| K1 回归 | 保留 recorder 对外签名·502 单测绿 |

## 9. 门禁与角色

- 预览页/评价卡 UI → `apple_ux_designer` 交互门禁(编码前)。
- K1b 健康评价文案 → `copywriter`(免责不恐吓)。
- K1b 发脱敏数据行为 → `apple_software_behavior`(告知/时机·配合 L1)。
- 每 Phase 编码后 → `google_quality_engineer` 终审。

## 10. 与验收合同映射 🔴

| 本文 | → 合同 |
|---|---|
| §4 接口契约 | §3 API 表面 |
| §8 红线(营养/免责/隐私) | §2 不变量 |
| §7 DoD+单测 | §7 测试台账 |
| §3 K1b 门禁(L1) | §5 时序(前置同意)+§8 例外 |
