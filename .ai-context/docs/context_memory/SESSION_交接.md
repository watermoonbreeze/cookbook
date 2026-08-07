# 🔖 SESSION 交接入口

> 更新时间：**2026-08-07（架构复核 + 颗粒度机制落地后）**
> 当前状态：**B4+B5+B6 架构模型复核未通过（9 项阻断）。同日建立"蓝图颗粒度分级机制"（L1~L7，35 条 GC）并已打补丁到 B4 蓝图。TURN=CODE（编码机），下一步直接读 B4 蓝图 §0.1 逐条落点开工，不必重新分析。**
> **协作模式：BLUEPRINT（C 档，常驻声明）**——开工前先读 `docs/context_memory/BLUEPRINT_STATE.md` 确认 `TURN` 是不是自己且看清楚"颗粒度"行；不是自己就停手，只报告持球方。
> 末位提交：待本次交接一并提交。上一提交 `599566e6`（声明协作模式 + 建立 BLUEPRINT_STATE）。

---

## 一、本轮完成（两件事，都是 ARCH = Claude@主力机 做的）

### 1.1 架构模型复核（B4+B5+B6）

对 B4+B5+B6 批次做架构模型终审，逐行核对 `a7fdf074..ac664fa1` 全部代码 diff（14 文件，2307 插入/341 删除，未采信 commit message 自述）。**结论：未通过**。完整报告：`docs/context_memory/架构模型复核报告_B4B5B6_2026-08-07.md`（9 项阻断 `AF-B456-01~09` + 3 项缺证据 + 13 项建议）。

### 1.2 蓝图颗粒度分级机制（用户当场提出的新需求，同日设计并落地）

用户诉求：审核发现的问题要能被系统性记录、可追踪、可复用，蓝图要有"颗粒度"分级（1~N，越大越精细），编码模型易犯的错要登记进级别，新增错误类别要能加级并说明原因。

- 先 spawn Opus 设计机制：**规模轴**（FULL/LITE，管写哪些工件）与**颗粒度轴**（`L1~L7`，管每个工件写多细）正交；GC（Granularity Clause）条款必须是存在性命题；三分支升级（复发→该 GC 复发计数+1/两次升自动检查；扩容→现有级别加条款；开新级→需要全新表达形式）。种子版 26 条 GC，直接用本次 9 项 AF 校验过。
- 用户追加：回溯 B1~B3 三轮复审 + 架构终审的结论（不看代码细节，只看结论），又挖出 9 条新 GC（GC-27~35）+ 3 个新 BL 类别。**最有价值的两个发现**：
  - `AF-B3-03`（B3，已修）→ `AF-B456-01`（B4，本次阻断）是**同一个 bug 跨批次真实复发**——本项目"编辑即失效"收口函数 `invalidateGenerationToInput` 第一次被 `setInputText()` 绕过（B3 修了），第二次被新入口 `setQuickDraft()` 绕过（B4 又踩）。→ `GC-27`。
  - `AF-ARCH-02`（构造时单例 parser 在段数=1 时被掩盖，段数>1 立即整体失效）是**全项目历史上最严重的单项阻断**。→ `GC-28`/`BL-09`。
- **落地文件**（本轮全部完成，见下方"三、本轮改动文件清单"）：共享规则 `blueprint_protocol.md`（已同步 ai-share）、项目 `12_多模型协作与实施蓝图规范.md`（新增 §12 GC 登记表 + §13 升级历史）、`BLUEPRINT_STATE.md`（新增"颗粒度"字段 + "CODE 入口"小节）、**B4 蓝图本身打补丁到 L7**（这是 CODE 下一步唯一要读的文件）。

---

## 二、⏭ 下一步（CODE 在编码机上按此执行，入口已内嵌进蓝图，不必再读本节以外的东西）

**唯一入口**：`docs/feature/AI记一餐_周期记_NDJSON流式_B4输入UI实施蓝图.md` **§0.1 颗粒度勾销表**。表里每行是一条 GC，"本蓝图落点"列直接指向你要改的章节（§1 末尾冻结值表、§3.5 索引空间不变量、§3.6 自动副作用表、§5.8 对象生命周期表、§7 步骤 2/3/6、§9.4），标"未满足·CODE 待办"的就是本批要关闭的点，**蓝图已给出唯一最小修复方案 + 完成形态字面量 + grep 判据，不必自己设计**。

- 遇到蓝图没写清楚的点 → 停手，按 `~/.ai-context/rules/blueprint_protocol.md` §3 记 `Q-B4-NN`，不得自行发挥。
- 关闭全部 9 项 AF 后：填 §7 步骤 6 STEP 勾销表 → §11.1 放行条件第 8 条逐项打勾（含 §0.1 表 35 条 GC 全部转"满足"）→ 蓝图头状态改回 `ACCEPTED` → `BLUEPRINT_STATE.md` 的 `TURN` 改回 `ARCH` → 同一提交 `git push`。
- 二次复核范围仅限本次 9 项 AF + 补充测试，ARCH 不会重新审查已通过项。

**旧版"复核通过条件 7 条"已被 §0.1 表取代**（内容一致，§0.1 更细、按 GC 逐条给了落点），不必再对照两份清单。

---

## 三、本轮改动文件清单（供快速核对，非必读）

| 文件 | 改动 |
|---|---|
| `~/.ai-context/rules/blueprint_protocol.md`（共享，已同步 ai-share） | §2 扩为两轴；新增 §2.1（颗粒度定义/判定式/GC书写红线/L1~L7语义）、§2.2（声明规范）；§4 加三分支升级判定 |
| `docs/experience/12_多模型协作与实施蓝图规范.md` | §2 BL 表加 BL-09/10/11；新增 §12（35 条 GC 完整登记表）+ §13（升级历史）；§11 模板补颗粒度声明位 |
| `docs/context_memory/BLUEPRINT_STATE.md` | 新增"颗粒度"字段行 + "CODE 入口"4 步小节 |
| `docs/feature/AI记一餐_周期记_NDJSON流式_B4输入UI实施蓝图.md` | 头部加颗粒度声明；新增 §0.1（勾销表）、§1 末尾（冻结值修订表）、§3.5（索引空间不变量，关 AF-05）、§3.6（自动副作用表，关 AF-04）、§5.8（对象生命周期表，关 AF-03）；§7 步骤 2 全面改写（关 AF-01/02）、步骤 3 拆 STEP（关 AF-09 + 截断提示落点）、新增步骤 6（STEP 勾销表）；§9.4（INV↔T 映射表）；§10 补归宿列；§11.1 加条件 8；§13 补 B6 真机分组 |
| `docs/experience/07_操作记录.md` / `INDEX.md` | 记录本轮（架构复核 + 颗粒度机制建立两条） |
| `docs/context_memory/架构模型复核报告_B4B5B6_2026-08-07.md`（新建） | 复核完整报告，9 阻断+3缺证据+13建议 |

---

## 四、先读清单（任何一端接手时按序读）

1. `BLUEPRINT_STATE.md`（**先读，确认 TURN + 颗粒度**）
2. `SESSION_交接.md`（本文件）
3. `docs/feature/AI记一餐_周期记_NDJSON流式_B4输入UI实施蓝图.md` §0.1（**CODE 的直接工作入口**）
4. 需要事故全貌时才读：`docs/context_memory/架构模型复核报告_B4B5B6_2026-08-07.md`（§0.1 已把结论摘到落点，通常不必整篇重读）
5. `.ai-context/PROJECT.md`
6. `docs/experience/12_多模型协作与实施蓝图规范.md` §12/§13（想理解某条 GC 为什么存在时查）

---

## 五、B4+B5+B6 代码文件速查（复核已确认的事实，本轮未改代码，仍准确）

| 文件 | 角色 | 本轮阻断涉及 |
|------|------|------|
| `shared/.../ai/meallog/AiMealPrompt.kt` | MAX_INPUT_CHARS 常量、maxTokens | §1 冻结值修订表待办 |
| `shared/.../ai/meallog/InputSegment.kt` | segmentId 唯一性 fail-fast | 缺配套测试 |
| `androidApp/.../ui/ai/AiMealInputViewModel.kt` | 状态机+进度+撤销，本轮改动最集中 | **AF-01/02（§7 步骤 2）** |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | Sheet 入口+各阶段组件 | **AF-02/03/09（§5.8、§7 步骤 3）** |
| `androidApp/.../ui/ai/PeriodDayBlock.kt` | 单天输入块 | AF-04（§3.6/§7 步骤 3.5） |
| `androidApp/.../ui/ai/SegmentProgressBar.kt` | 段进度条 UI | **AF-05（§3.5，假修复过一次，别重蹈）** |
| `androidApp/.../ui/ai/GenerationProgress.kt` | 段进度数据类 | AF-05 需扩 `segmentStatuses` 字段 |
| `androidApp/.../ui/addmeal/AddMealViewModel.kt` / `AddDayFoodScreen.kt` | 已有餐食查询 / UI 冻结修复 | AF-08（§13 真机登记） |

**不改**（B1-B6 全程零改动，本轮已验证）：`StreamingMealSession`、`StreamingMealParser`、`MealStreamDraftMapper`、`CloudAiRuntime`、`StreamTransport`、Repository、SQLDelight、DI。

---

## 六、关键红线（累加不变，本轮新增见末尾三条）

同 B3/B4/B5 交接 + 本轮新增：
- segmentId 唯一性 fail-fast
- 200 字截断在 VM 层——但截断后**必须提示**，静默截断是阻断（见 §3.6）
- 草稿隔离——但重叠字段**不得并存无同步**（见 §7 步骤 2.0/2.1）
- preview 仅在段终态 + final 触发
- ✅ 新增字段先 grep 旧字段全部写入点；列表逐项状态禁用计数+下标反推
- ✅ **本轮新增**：项目已有"编辑即失效"收口函数（`invalidateGenerationToInput`）时，新增编辑入口必须核对是否路由过它（`GC-27`，B3→B4 真实复发过一次）
- ✅ **本轮新增**：构造时创建、后续多次迭代复用的对象/字段，扩展迭代基数（1→N）前必须显式回答是否要按基数分片（`GC-28`）
- ✅ **本轮新增**：蓝图颗粒度不得下调；不适用的 GC 标 `N/A+理由`，不是省略（见 §0.1 表）

---

## 七、架构模型复核检查点（状态更新）

> 复核已执行，**结论未通过**。TURN 已转 CODE。CODE 按 B4 蓝图 §0.1 完成全部"未满足"项后，交回 ARCH 做二次复核（范围仅限本次 9 项 AF + 补充测试，不重新审查已通过项）。
