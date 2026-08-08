# K1e：全App AI调用点「语义→AI专用结构」转换层 实施蓝图

> 状态：`DISCARDED`（2026-08-08 GC-37 独立挑战证伪本批价值前提，用户裁定废弃，不实施）
>
> **废弃原因**：`待办_功能算法.md` 对 K1e 的原始判断"紧凑JSON短key替代中文标签行，省50-70% token"经独立挑战 agent 按现有代码逐字符估算证伪——`RecommendationPrompt.kt`/`PlanOrchestrator.kt` 现状的候选渲染**本就是条件式输出**（空字段不写，非"总是输出所有字段"的臃肿格式），且中文单/双字标签（如"｜主料:"5字符）本身就比 JSON 语法（`,"main":[...]`10+字符，每个字符串都要加引号+逗号+冒号+方括号）更紧凑。抽样估算显示改紧凑 JSON 后 user 提示词**净增约 60~80% 字符/token**，方向与 backlog 预期完全相反。另发现改造会与既有 system 提示词里 6+3 处"候选已标『荤/素/主食』『利调养』『注意限量』"等文案产生语义冲突（那些中文标签词在紧凑格式下已不存在，AI 会被要求寻找不存在的标签）。
>
> **结论**：K1e 这一backlog条目的问题定位本身有误，不是"蓝图需要改进"，是"这件事不该做"。`AiMealPrompt`（NDJSON 主 Prompt）本就被本蓝图列为 Out of Scope（已调优冻结区），故 K1e 三个候选调用点里，两个（推荐/周计划）被证伪，一个（记一餐）从未打算做——整项废弃。本文件保留作为调研记录，避免未来重复踏入同一个想当然的"AI结构应该更紧凑"直觉陷阱；不进入实施、不移交 CODE。
>
> 以下正文是废弃前的完整设计草案，仅供追溯参考。
>
> ---
>
> **本蓝图仅由 ARCH 起草，不含任何代码实现**——按用户 2026-08-08 指示"你只负责蓝图，不要编码"。
> **颗粒度：L7**（项目基线，§0.1 大量 N/A——本批是纯函数级 Prompt 编码格式改造，不涉及 UI/状态机/并发/持久化）。
> 起草日期：2026-08-08（L1 蓝图转 `BLUEPRINT_READY` 并交 CODE 后，按用户既定顺序 L1→K1e→K1i→K1h 起草第二项）。
> 前置门禁核查：本批**不涉及**界面/交互改动（不改任何 Compose 文件、不改用户可见文案结构），**不涉及**"App 自动行为"新增（AI 调用的触发时机/频率/结果消费方式完全不变，只改"喂给 AI 的候选菜怎么编码"），故豁免 `apple_ux_designer`/`apple_software_behavior` 前置设计门禁。

---

### §0.1 颗粒度勾销表（GRANULARITY = L7）

| GC | 条款一句话 | 本蓝图落点 | 状态 |
|---|---|---|:--:|
| GC-01 | 每个行为分支写成"条件→唯一动作→禁止动作" | §3 不变量表 | 满足 |
| GC-02 | allowlist：文件×允许操作×禁止操作 + 显式禁改清单 | §6 | 满足 |
| GC-03 | 上一批延后项归宿表 | §1.4 | 满足：与本批无功能交集 |
| GC-04 | 每条 INV 五列齐全 | §3 | 满足 |
| GC-05 | INV↔T 双向映射表 | §8.2 | 满足 |
| GC-06 | 放行条件写命令原文 | §7 末尾 | 满足 |
| GC-07 | 测试夹具职责边界表 | §8.1 | 满足 |
| GC-08 | 真机清单文件名+编号区间 | §9 | 满足（本批新增真机项仅"AI 输出质量抽查"一类，非常规功能验证，见 §8.2 说明） |
| GC-09 | 回归基线锁定 | §6 末尾 | 满足 |
| GC-10 | 逐字段真相源表 | §4.1 | 满足 |
| GC-11 | 字段/结构迁移清单 | §4.2 | 满足（`RecommendationPrompt.build()`/`PlanOrchestrator.buildPrompt()` 的候选渲染逻辑替换，含全部读写点） |
| GC-12 | UI 判据与业务判据同源 | — | N/A：本批不产生任何新 UI 状态/判据，AI 响应解析入口（`RecommendationResult`/`RawPlan` 反序列化）完全不变 |
| GC-13 | fallback 复用主路径校验入口 | — | N/A：本批不新增 fallback 路径，AI 响应格式（输出侧）不变，规则兜底逻辑不变 |
| GC-14~16 | 对象生命周期/可变持有物/搬迁注释 | — | N/A：纯函数改造，无跨 Composable 持有物、无系统资源对象 |
| GC-17~19 | 逐项状态 List&lt;Status&gt; | — | N/A：无状态机 |
| GC-20 | 自动副作用清单 | — | N/A：本批不产生任何新的自动截断/丢弃/降级行为——候选菜数量/内容/AI 可选范围完全不变，只改"如何描述给 AI" |
| GC-21 | INV 写"提示/告知"必须有 STEP 落点 | — | N/A：本批无用户可见提示新增 |
| GC-22 | 可见副作用配 T-ID | — | N/A：同上 |
| GC-23 | STEP 独立编号+完成形态字面量 | §7 | 满足 |
| GC-24 | STEP 勾销表 | §9 | 满足 |
| GC-25 | 完成形态字面量+grep判据 | §7 | 满足 |
| GC-26 | 冻结值修订记录 | — | N/A：本批不改任何健康/营养/推荐阈值常量，只改候选菜的"描述格式" |
| GC-27 | 编辑即失效收口函数核对 | — | N/A：与 AI 记一餐编辑态无关 |
| GC-28 | 构造时创建对象按基数分片 | — | N/A：`AiCompactJson`/编码函数均为无状态单例/纯函数，非"构造时创建、后续复用"的累积对象 |
| GC-29 | 多来源写入同一聚合目标 | — | N/A：本批新增结构体只有唯一写入者（各自的 `toCompact()` 扩展函数） |
| GC-30 | 状态转移驱动完整副作用链 | — | N/A：无状态转移 |
| GC-31 | 挂起点+身份重校验 | — | N/A：本批全部是同步纯函数，`RecommendationPrompt.build()`/`PlanOrchestrator.buildPrompt()` 本身非 suspend，不新增挂起点 |
| GC-32 | 高频异步事件节流 | — | N/A：无新增异步事件 |
| GC-33 | 禁止测试专用可变全局注入点 | — | N/A：纯函数天然可测，不需要注入点 |
| GC-34 | 注释/KDoc 与实现一致性 | §7 各 STEP | 满足 |
| GC-35 | 协议事件枚举逐项对照 | — | N/A：AI **输出**侧的 JSON 协议（`{"suggestions":[...]}"`/`{"days":[...]}"`）本批不改，只改**输入**侧候选编码 |
| GC-36 | List&lt;Status&gt; 值域覆盖 | — | N/A：无状态列表 |
| GC-37 | 蓝图冻结前独立挑战台账 | §10 | 待跑 |

任一条"未满足"须先处理。当前 GC-37 待跑，其余全部满足。

---

## §1 目标与范围

### 1.1 一句话价值

把喂给云端 AI 的候选菜列表，从"每道菜一行中文标签文本"（如"- id=123 红烧肉｜荤｜主料:五花肉｜在手:五花肉｜利调养:低脂"）改成"短 key 紧凑 JSON 数组"（如`{"id":123,"n":"红烧肉","m":1,"main":["五花肉"],"hand":["五花肉"],"rec":["低脂"]}`），候选越多（通常几十道菜）省的 token 越多，AI **输出**格式与业务逻辑完全不变。

### 1.2 触发来源

- **K1e**（`待办_功能算法.md` 🔴⬜）：全 App AI 调用点「语义→AI专用结构」转换层，3 个 AI 调用点 token 浪费严重。
- `待办总览.md` 全局审计定位到 3 个具体调用点：`RecommendationPrompt`（最大浪费源，每候选一行标签文本）、`PlanOrchestrator`（同构问题）、`AiMealPrompt`（system prompt 本身冗长）。

### 1.3 In Scope / Out of Scope

**In Scope**：
1. 新增共享编码工具 `shared/.../ai/AiCompactCoding.kt`：`AiCompactJson`（`encodeDefaults=false` 的 `Json` 单例）+ `CompactCandidate`/`CompactPlanDish` 两个短 key `@Serializable` 数据类 + 对应 `toCompact()` 扩展函数。
2. `RecommendationPrompt.build()`：候选菜渲染从手写字符串拼接改为 `AiCompactJson.encodeToString(candidates.map{it.toCompact(...)})`；system prompt 追加一段紧凑 schema 图例（一次性固定文本，非随候选数量增长）。
3. `PlanOrchestrator.buildPrompt()`：同构改造，候选菜渲染改用 `CompactPlanDish`。
4. **不改** AI 响应侧解析逻辑（`RecommendationResult`/`PlanOrchestrator.RawPlan` 等反序列化目标不变）——AI 仍然输出 `{"suggestions":[{"dishIds":[...],...}]}` / `{"days":[...]}"`，只是"读候选"这一侧的格式变了。
5. 同步更新 `RecommendationPromptTest.kt` 的断言方式（从 `substringAfter/substringBefore` 字符串探测改为解析 JSON 后按字段断言，更精确也更抗格式变化）。

**Out of Scope（本批不做，理由见括号）**：
- **`AiMealPrompt`（AI 记一餐 NDJSON/FLAT system prompt）**——该文件是 K1 系列 B1~B6 六个批次反复调参、真机验证过的战果，K1a/K1b 两份蓝图都把它列为高风险冻结区；`AI快捷记一餐_进阶_架构方案.md:31` 原始设计也明确"K1e 全 App AI 结构转换层…本方案仅在 K1b prompt 复用 K1e 紧凑结构思路，不整体做"——即从产品设计之初就没打算把 K1e 套进这个已调好的 prompt。本批遵循这一既定判断，不碰。若未来确有必要压缩，应作为独立、单独验证过 NDJSON 解析准确率不回退的批次，不与本批捆绑。
- **AI 响应侧协议格式压缩**（如把输出也改紧凑短 key）——响应体积远小于候选列表（几十道菜 vs 几条建议），压缩收益低，且改动响应格式意味着要同步改全部解析代码，风险/收益比不划算，本批只压缩"喂给 AI 看"的输入侧。
- **AI 输出质量的自动化回归**（"压缩后推荐是否还一样好"这件事无法用单元测试判定，只能测"JSON 格式没变、可解析、内容传达完整"）——AI 是否因为格式改变而理解力下降，需要真机/人工抽查几组真实输出，登记为真机项而非自动化测试项。

### 1.4 上一批延后项归宿核对（GC-03）

无交集。

---

## §2 现状与差距

| # | 现状 | 证据（file:line） | 差距/影响 |
|---|---|---|---|
| D1 | `RecommendationPrompt.build()` 每道候选菜手写拼接一行中文标签文本（`- id=X name｜荤｜主料:…｜在手:…｜利调养:…`） | `RecommendationPrompt.kt:63-88` | ~50 道候选菜时这段文本可达数千 token，且大量重复的中文标签词（"｜主料:"、"｜在手:" 等）本身就是纯粹的格式开销，不含信息量 |
| D2 | `PlanOrchestrator.buildPrompt()` 候选菜渲染同构问题 | `PlanOrchestrator.kt:114-124` | 同上，且两处渲染逻辑各写一遍，未来任一处的候选字段变化都要改两遍 |
| D3 | AI 响应侧解析（`RawPlan`/`RecommendationResult`）完全不依赖候选的渲染格式——候选只是"喂给 AI 看的参考资料"，AI 回的是 `dishIds` 引用，不回显候选原文 | `PlanOrchestrator.kt:171-178`（`RawPlan`/`RawDay`/`RawMeal` 只含 `dishIds`，不含候选字段回显） | 这正是本批改动"零风险扩散"的关键事实：改候选编码格式不会触及任何下游解析代码，改动面天然收窄到"渲染函数内部" |

---

## §3 不变量

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-K1E-01 | `RecommendationPrompt.build()` 渲染候选菜列表 | 用 `AiCompactJson.encodeToString(candidates.map { it.toCompact(preferenceScores, nutritionBalanceScores) })` 产出一个 JSON 数组字符串，嵌入 `user` 提示词；候选与 AI 可选范围（`dishIds` 只能引用给出的 id）语义完全不变 | 改变候选集合本身（过滤/增减候选）；让 AI 可选范围超出/少于原候选集合 | T-K1E-01 |
| INV-K1E-02 | `CompactCandidate` 序列化 | 默认值字段（`m=0`/`s=0`/`c=1`/空数组/`freq=0`/`nb=0`/`recent=null`）在 `encodeDefaults=false` 下从 JSON 输出中省略；非默认值字段正常输出 | 任何默认值字段被误配置为"不可省略"（会让省略带来的 token 收益落空） | T-K1E-02 |
| INV-K1E-03 | `RecommendationPrompt.build()` 的 `system` 提示词 | 追加一段一次性的紧凑 schema 图例（说明 `id`/`n`/`m`/`s`/`c`/`main`/`hand`/`hSzn`/`rec`/`lim`/`freq`/`nb`/`recent`/`miss` 各字段含义，含"字段不出现即默认值"这条通用规则），**不随候选数量重复** | 把图例文字混进每条候选（那样图例开销会随候选数线性放大，失去"只加一次"的省 token 意义） | T-K1E-03 |
| INV-K1E-04 | `PlanOrchestrator.buildPrompt()` 渲染候选菜列表 | 用 `CompactPlanDish` 同构改造，system 追加对应图例（字段：`id`/`n`/`b`/`soft`/`nut`/`season`/`h`） | 同 INV-K1E-01 的禁止结果 | T-K1E-04 |
| INV-K1E-05 | 既有 R5（禁医疗断言）/R2（近吃标签）/组合完整性（荤素主食）/重油度分级/口味汇总 等既有 system 提示词内容 | 全部原样保留，不因本批改动而删除或弱化任何一条既有行为约束 | 借本批"顺手"精简/删除任何一条既有约束文案 | T-K1E-05（复用/改写现有 `RecommendationPromptTest.kt` 全部既有断言，改为 JSON 结构断言但断言的行为语义不变） |

---

## §4 接口契约

### 4.1 逐字段真相源表

| 字段/结构 | 唯一写入者 | 读取方 | 终局形态 |
|---|---|---|---|
| `CompactCandidate`/`CompactPlanDish` | 各自的 `toCompact()` 扩展函数（一次性构造，非累积） | `AiCompactJson.encodeToString(...)` | 新增，仅供 Prompt 渲染内部使用，不持久化、不跨函数传递 |
| `RecommendationPrompt.build()`/`PlanOrchestrator.buildPrompt()` 的 `user` 字符串 | 各自函数内部一次性 `buildString{}` | `LlmRequest.user` → 网络请求 | 内部渲染逻辑替换，外部签名不变 |

### 4.2 GC-11：候选渲染逻辑迁移清单

**`RecommendationPrompt.build()` 候选渲染（`kt:67-88`）**：

| 现状写法 | 迁移后处置 |
|---|---|
| `candidates.forEach { c -> append("- id=")... }` 手写拼接，含 12 个条件 append 分支 | 整体替换为 `CompactCandidate` 序列化；12 个条件分支逐一映射为 `CompactCandidate` 的对应字段（见 §4.4 完整映射表），**无遗漏字段**（GC-11 要求逐项处置，不得"顺手"丢掉任何一个既有标注维度） |

**`PlanOrchestrator.buildPrompt()` 候选渲染（`kt:116-124`）**：同构处置，映射见 §4.4。

**终局裁决**：**替换**（不并存两套渲染逻辑）。理由：候选渲染是纯粹的"内部实现细节"，不是对外可见字段，无兼容负担。

### 4.3 GC-12：N/A（见 §0.1）

### 4.4 新增类型与函数签名

```kotlin
// shared/.../ai/AiCompactCoding.kt（新文件）
package com.sxdbsm.cookbook.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.sxdbsm.cookbook.ai.model.DishCandidate
import com.sxdbsm.cookbook.ai.model.PlanDish

/** AI 提示词专用紧凑 JSON 编码：默认值省略，省 token。[AI生成] K1e */
object AiCompactJson {
    val instance: Json = Json { encodeDefaults = false }
}

/**
 * 云端推荐候选的紧凑编码（对应 [RecommendationPrompt]）。[AI生成] K1e
 *
 * 字段含义供 system 提示词图例引用（见 [RecommendationPrompt.COMPACT_SCHEMA_LEGEND]）：
 * id=菜id, n=菜名, m=1荤(省略=素), s=1主食, c=0清淡/2重口(省略=中性),
 * main=主料, hand=在手食材, hSzn=在手调料, rec=利调养食材, lim=需注意限量食材,
 * freq=1常做, nb=1一般补/2强补营养, recent=N天前吃过(0=今天), miss=还缺的食材。
 */
@Serializable
data class CompactCandidate(
    val id: Long,
    val n: String,
    val m: Int = 0,
    val s: Int = 0,
    val c: Int = 1, // cookingHeaviness() 原始返回值：0清淡/1中性/2重口，中性(1)为默认省略
    val main: List<String> = emptyList(),
    val hand: List<String> = emptyList(),
    val hSzn: List<String> = emptyList(),
    val rec: List<String> = emptyList(),
    val lim: List<String> = emptyList(),
    val freq: Int = 0,
    val nb: Int = 0,
    val recent: Int? = null,
    val miss: List<String> = emptyList(),
)

/** [AI生成] K1e：DishCandidate → CompactCandidate，逐字段对照 §4.4 映射表，NB_STRONG 阈值判据不变（复用 RecommendationPrompt 现有常量）。 */
fun DishCandidate.toCompact(preferenceScores: Map<Long, Double>, nutritionBalanceScores: Map<Long, Double>): CompactCandidate {
    val nb = nutritionBalanceScores[id] ?: 0.0
    return CompactCandidate(
        id = id, n = name,
        m = if (isMeat) 1 else 0,
        s = if (isStaple) 1 else 0,
        c = cookingHeaviness(cookingMethodNames),
        main = mainNames,
        hand = onHandNames,
        hSzn = seasoningsOnHand,
        rec = recommendHits,
        lim = limitHits,
        freq = if ((preferenceScores[id] ?: 0.0) >= 0.5) 1 else 0,
        nb = if (nb >= RecommendationPrompt.NB_STRONG) 2 else if (nb > 0.0) 1 else 0,
        recent = recentDaysAgo,
        miss = missingNames,
    )
}

/**
 * 周期规划候选的紧凑编码（对应 [PlanOrchestrator]）。[AI生成] K1e
 *
 * 字段含义：id=菜id, n=菜名, b=1早餐(省略=正餐), soft=1早餐软/饮(仅早餐菜有意义，省略=硬/主食),
 * nut=营养标签(至多3个), season=1应季, h=1利健康。
 */
@Serializable
data class CompactPlanDish(
    val id: Long,
    val n: String,
    val b: Int = 0,
    val soft: Int = 0,
    val nut: List<String> = emptyList(),
    val season: Int = 0,
    val h: Int = 0,
)

/** [AI生成] K1e：PlanDish → CompactPlanDish，逐字段对照 §4.4 映射表。 */
fun PlanDish.toCompact(currentSeason: String): CompactPlanDish = CompactPlanDish(
    id = id, n = name,
    b = if (isBreakfast) 1 else 0,
    soft = if (isBreakfast && breakfastSoft) 1 else 0,
    nut = nutritionTags.take(3).toList(),
    season = if (currentSeason in seasonTags || "应季" in seasonTags) 1 else 0,
    h = if (isHealthy) 1 else 0,
)
```

**§4.4 完整字段映射表（GC-11 逐项核对，不得遗漏）**：

| 现状标注（RecommendationPrompt） | CompactCandidate 字段 |
|---|---|
| `｜荤`/`｜素` | `m` |
| `｜主食` | `s` |
| `｜清淡`/`｜重口`（中性不标） | `c` |
| `｜主料:X` | `main` |
| `｜在手:X` | `hand` |
| `｜在手调料:X` | `hSzn` |
| `｜利调养:X` | `rec` |
| `｜常做` | `freq` |
| `｜补营养✓✓`/`｜补营养✓` | `nb` |
| `｜N天前吃过`/`｜今天吃过` | `recent` |
| `｜还差:X` | `miss` |
| `｜注意限量:X` | `lim` |

| 现状标注（PlanOrchestrator） | CompactPlanDish 字段 |
|---|---|
| `｜[早餐]`/`｜[正餐]` | `b` |
| `(软/饮)`/`(硬/主食)` | `soft` |
| `｜营养:X`（取前3） | `nut` |
| `｜应季` | `season` |
| `｜[利健康]` | `h` |

```kotlin
// shared/.../ai/RecommendationPrompt.kt（system 追加图例常量 + user 渲染替换）
object RecommendationPrompt {
    // NB_STRONG 改 internal（原 private），供 toCompact() 复用同一阈值判据，避免重复定义（单一真相源）。
    internal const val NB_STRONG = 0.5

    /** [AI生成] K1e：紧凑候选 schema 图例，system 内只出现一次，不随候选数量重复。 */
    private const val COMPACT_SCHEMA_LEGEND =
        "候选菜为紧凑JSON数组，字段含义：id=菜id，n=菜名，m=1时为荤菜(不出现即素菜)，s=1时为主食，" +
            "c=0清淡/2重口(不出现即中性)，main=主料，hand=在手食材，hSzn=在手调料，rec=利调养食材，" +
            "lim=需注意限量食材，freq=1时为常做，nb=1一般补营养/2强补营养，recent=N天前吃过本菜(0=今天)，miss=还缺的食材。"

    fun build(/* 签名不变 */): LlmRequest {
        val system = buildString {
            // ... 既有全部内容原样保留（R5/R2/组合完整性/重油度/口味汇总等，见 INV-K1E-05）...
            append(COMPACT_SCHEMA_LEGEND)
            append("严格输出 JSON，不要多余文字。")
        }
        val user = buildString {
            if (constraints.labels.isNotEmpty()) { /* 不变 */ }
            append("候选菜（只能用这些 id）：\n")
            append(AiCompactJson.instance.encodeToString(candidates.map { it.toCompact(preferenceScores, nutritionBalanceScores) }))
            append("\n\n只输出如下 JSON：\n")
            append("""{"suggestions":[{"dishIds":[菜id,...],"reason":"一句理由","cookingHint":"做法建议"}]}""")
        }
        return LlmRequest(system = system, user = user)
    }
}
```

```kotlin
// shared/.../ai/PlanOrchestrator.kt（buildPrompt 同构改造）
private fun buildPrompt(/* 签名不变 */): LlmRequest {
    val system = buildString {
        // ... 既有全部内容原样保留 ...
        append("候选菜为紧凑JSON数组，字段含义：id=菜id，n=菜名，b=1为早餐菜(不出现即正餐)，" +
            "soft=早餐软硬(1软/饮,不出现即硬/主食,仅早餐菜有意义)，nut=营养标签(至多3个)，season=1为应季，h=1为利健康。")
        append("严格输出 JSON，不要多余文字。")
    }
    val user = buildString {
        append("当前季节：${ctx.season}。候选菜（只能用这些 id）：\n")
        append(AiCompactJson.instance.encodeToString(ctx.dishes.map { it.toCompact(ctx.season) }))
        append("\n\n只输出如下 JSON：\n")
        append("""{"days":[{"meals":[{"name":"餐次名","dishIds":[菜id,...]}]}]}""")
    }
    return LlmRequest(system = system, user = user)
}
```

---

## §5 UI 设计

不适用（本批不涉及任何 UI/Compose 改动，见蓝图头部门禁核查）。

---

## §6 文件改动清单 + Allowlist

| 文件 | 允许操作 | 禁止操作 |
|---|---|---|
| 新建 `shared/.../ai/AiCompactCoding.kt` | 新建（`AiCompactJson`/`CompactCandidate`/`CompactPlanDish`/两个 `toCompact()` 扩展） | — |
| `shared/.../ai/RecommendationPrompt.kt` | `build()` 内候选渲染替换为紧凑 JSON；`system` 追加图例常量；`NB_STRONG` 可见性改 `internal` | 改既有 R5/R2/组合完整性/重油度/口味汇总等任何一条既有 system 提示词内容（只能"追加图例"，不能"删改既有约束"）；改函数签名 |
| `shared/.../ai/PlanOrchestrator.kt` | `buildPrompt()` 内候选渲染替换为紧凑 JSON；`system` 追加图例 | 改 `mergeAiWithRule`/`parseAiDays`/`plan()` 等其余逻辑；不改函数签名 |
| `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/ai/RecommendationPromptTest.kt` | 重写全部既有测试的断言方式（`substringAfter/substringBefore` → 解析 `req.user` 里的 JSON 数组后按字段断言），**断言的行为语义（R5/R2/组合完整性/重油度/口味汇总）不得删减** | 删除任何一条既有测试用例（只能"改断言写法"，不能"减少覆盖范围"） |
| 新建 `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/ai/AiCompactCodingTest.kt` | 新增 T-K1E-01~05 里"编码正确性"相关断言 | — |

**显式禁改文件清单**：
- `shared/.../ai/meallog/AiMealPrompt.kt`（明确排除，见 §1.3）
- `shared/.../ai/PeriodPlanner.kt`（规则兜底路径，不涉及 AI 编码）
- `shared/.../ai/RecommendationOrchestrator.kt`（AI 响应解析，本批不改响应格式）
- `androidApp/.../ui/ai/*`（本批不涉及任何 UI 层）

**回归基线锁定（GC-09）**：
- `:shared:testDebugUnitTest`（全量，0 failures）
- `:androidApp:assembleDebug`

---

## §7 分阶段实施步骤

### 批 K1E-1：共享编码工具

**STEP-K1E-1.1**：新建 `shared/.../ai/AiCompactCoding.kt`，按 §4.4 完整实现。
完成形态：`grep "object AiCompactJson" AiCompactCoding.kt` 命中 1 处；`grep "data class CompactCandidate" AiCompactCoding.kt` 命中 1 处；`grep "data class CompactPlanDish" AiCompactCoding.kt` 命中 1 处；`grep "fun DishCandidate.toCompact" AiCompactCoding.kt` 命中 1 处；`grep "fun PlanDish.toCompact" AiCompactCoding.kt` 命中 1 处。

### 批 K1E-2：RecommendationPrompt 改造

**STEP-K1E-2.1**：`RecommendationPrompt.kt` 的 `NB_STRONG` 可见性 `private`→`internal`。
完成形态：`grep "internal const val NB_STRONG" RecommendationPrompt.kt` 命中 1 处。

**STEP-K1E-2.2**：`RecommendationPrompt.kt` 按 §4.4 新增 `COMPACT_SCHEMA_LEGEND` 常量，`system` 内追加（放在既有全部约束文案之后、"严格输出 JSON"之前）。
完成形态：`grep "COMPACT_SCHEMA_LEGEND" RecommendationPrompt.kt` 命中 ≥2 处（定义+引用）。

**STEP-K1E-2.3**：`build()` 的 `user` 候选渲染（`kt:67-88` 的 `candidates.forEach{...}` 整块）替换为 `AiCompactJson.instance.encodeToString(candidates.map{it.toCompact(preferenceScores, nutritionBalanceScores)})`。
完成形态：`grep "candidates.forEach { c ->" RecommendationPrompt.kt` 零命中；`grep "AiCompactJson.instance.encodeToString(candidates.map" RecommendationPrompt.kt` 命中 1 处。

### 批 K1E-3：PlanOrchestrator 改造

**STEP-K1E-3.1**：`PlanOrchestrator.kt` 的 `buildPrompt()` 按 §4.4 新增图例文案 + 候选渲染替换（`kt:116-124` 的 `ctx.dishes.forEach{...}` 整块）。
完成形态：`grep "ctx.dishes.forEach { d ->" PlanOrchestrator.kt` 零命中；`grep "AiCompactJson.instance.encodeToString(ctx.dishes.map" PlanOrchestrator.kt` 命中 1 处。

### 批 K1E-T：测试

**STEP-K1E-T-1**：新建 `AiCompactCodingTest.kt`，覆盖编码正确性（见 §8.2 T-K1E-01/02/04）。

**STEP-K1E-T-2**：重写 `RecommendationPromptTest.kt` 全部既有测试断言方式（见 §6 allowlist，改断言写法不减覆盖），新增图例存在性断言（T-K1E-03）。

**验收命令**：
```
scripts\build-cli.bat :shared:testDebugUnitTest
scripts\build-cli.bat :androidApp:assembleDebug
```

---

## §8 测试矩阵

### 8.1 测试夹具职责边界（GC-07）

| 夹具 | 职责 | 禁止 |
|---|---|---|
| 无需 fake/mock——`toCompact()`/`AiCompactJson` 均为纯函数，直接用真实 `DishCandidate`/`PlanDish` 构造入参 | — | — |

### 8.2 INV↔T 双向映射表

| INV | T-ID | 断言要点 |
|---|---|---|
| INV-K1E-01 | T-K1E-01 | 构造若干 `DishCandidate`，断言 `RecommendationPrompt.build().user` 内可解析出与候选数量一致的 JSON 数组，且每个元素的 `id` 与传入候选一一对应 |
| INV-K1E-02 | T-K1E-02 | `CompactCandidate(id=1,n="x")`（全默认值）序列化后 JSON 字符串**不含** `"m"`/`"s"`/`"main"` 等默认字段 key；非默认值（如 `m=1`）序列化后**含** `"m":1` |
| INV-K1E-03 | T-K1E-03 | `system` 内 `COMPACT_SCHEMA_LEGEND` 只出现一次，不随候选数量变化（用 2 组不同候选数量的请求断言 `system` 长度相同/图例出现次数均为 1） |
| INV-K1E-04 | T-K1E-04 | 同 T-K1E-01/02，对象换成 `PlanDish`/`CompactPlanDish` |
| INV-K1E-05 | T-K1E-05 | 重写后的 `RecommendationPromptTest.kt` 全部既有 5 个测试方法（R5/R2/组合完整性/重油度分级/口味汇总）改断言写法后依然全部通过，断言的行为语义（如"红烧肉应标重口"）不变，只是断言手段从字符串子串改为"解析 JSON 后取该候选对象的 `c` 字段等于 2" |
| — | 真机 E-K1E-01（GC-08，非常规功能验证） | 配置真实云端 AI，分别用改造前/改造后各跑一次真实推荐请求，人工比对两次输出的候选覆盖/建议质量无明显下降（AI 是否因紧凑格式理解力下降，无法自动化判定，登记为一次性人工抽查项，非持续回归项） |

---

## §9 交付台账（CODE 完成时填）

### STEP 勾销表

| STEP-ID | 状态 | 落地 commit | diff 定位 |
|---|---|---|---|
| STEP-K1E-1.1 | ⬜ | | |
| STEP-K1E-2.1~2.3 | ⬜ | | |
| STEP-K1E-3.1 | ⬜ | | |
| STEP-K1E-T-1~2 | ⬜ | | |

### 验收命令输出 / 真机待验证登记

（交付时填；`E-K1E-01` 登记至时间戳最新的真机待验证清单）

---

## §10 独立挑战台账（GC-37）

（待跑：需换一次独立视角重新挑战本蓝图，重点核对：① `CompactCandidate`/`CompactPlanDish` 的字段映射表（§4.4）是否真的逐一覆盖了现状渲染逻辑的全部 12+5 个标注维度，有无遗漏或错位；② `kotlinx.serialization` 的 `encodeDefaults=false` 在 `List<String> = emptyList()` 这类集合默认值上是否真的按预期省略（不同版本/配置下可能有边界差异）；③ system 提示词追加图例后，整体 system 长度是否仍在既有 `maxTokens`/上下文窗口预算内（尤其 `PlanOrchestrator` 已有较长既有约束文案）；④ `RecommendationPromptTest.kt` 重写后是否真的保留了全部既有断言的等价覆盖，而非"看起来测了但漏了某个分支"。挑战完成后本节补齐，蓝图方可从 `DRAFT` 转 `BLUEPRINT_READY`。）

---

## §11 门禁与角色

- 本批不涉及 UI/交互/App 自动行为，豁免 `apple_ux_designer`/`apple_software_behavior` 前置门禁（见蓝图头部说明）。
- CODE 完成、构建+单测通过后，仍须走 `google_quality_engineer` 代码质量终审（项目强制门禁，无豁免条件）——重点关注紧凑编码是否真的省了预期的 token 比例（可选：终审时贴一组真实候选数量下改造前后的字符数对比作为证据）。
- 本批不涉及文案/合规内容，豁免 `copywriter` 审校。

## §12 弃置项登记（GC-03 前瞻）

| 项 | 状态 | 归宿 |
|---|---|---|
| `AiMealPrompt`（AI 记一餐 NDJSON/FLAT system prompt）紧凑化 | 显式弃置 | 已调优冻结区，独立评估批次，不与本批捆绑（见 §1.3） |
| AI 响应侧协议压缩 | 显式弃置 | 收益/风险比不划算，见 §1.3 |
