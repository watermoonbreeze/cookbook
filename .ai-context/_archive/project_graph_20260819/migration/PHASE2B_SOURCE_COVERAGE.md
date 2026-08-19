# Phase 2B Source Coverage Ledger

> **Migration Audit Artifact（对账审计产物）——不是 Project Truth。**
> 本轮（Migration Reconciliation）对每个 Source 文件的可行动事项逐一给出 disposition，证明：
> **ACTIONABLE SOURCE ITEMS → 100% DISPOSITION → UNEXPLAINED = 0**。
> 本文件不参与 Project Graph 语义校验，是人工/工具可审计的对账台账。
>
> 执行基准：`6152e8f373e1e006cbfcadbf7bbb9cec03de3e7e`
> 生成日期：2026-08-10（Phase 2B Rework）
>
> Disposition 类型见实施蓝图 §9；禁用的过滤逻辑见 §10。

---

## 1. Source 文件清单（重新枚举，非只读上一版 Inventory）

扫描 `.ai-context/docs/feature/` 实际文件：

| # | Source 文件 | 角色 |
|---|------------|------|
| S1 | `待办索引.md` | 全局入口 · 发现 + 关联专项 |
| S2 | `待办_Bug修复.md` | Bug 台账 |
| S3 | `待办_功能算法.md` | 功能/算法/AI 台账 |
| S4 | `待办_UI交互.md` | UI/UX/文案台账 |
| S5 | `待办_数据健康.md` | 数据/健康/营养台账 |
| S6 | `待办_工程合规.md` | 工程/性能/合规/平台台账 |
| S7 | `待办_战略会商.md` | 战略/会商/方案讨论台账 |
| S8 | `待办总览.md` | 历史存档（2026-08-01 已拆分，不再更新） |
| S9 | `待办执行分组_2026-08-04.md` | 执行顺序分组（非独立台账，引用专项） |
| S10 | `工程优化待办.md` | 工程优化专项（P0-P5） |
| S11 | `UX深挖审计与待办.md` | UX 审计专项（2026-07-14） |

状态 Truth 来源：`SESSION_交接.md` + `BLUEPRINT_STATE.md` + 最新真机验证清单。

---

## 2. S1 `待办索引.md` —— 索引层，专项承载身份

待办索引是发现层，各专项待办更完整 → **身份/内容以专项为准**（实施蓝图 §45）。索引中所有条目已在对应专项中处置，此处给出映射锚点。

| SOURCE ITEM | TITLE | DISPOSITION | TARGET | NOTES |
|---|---|---|---|---|
| 高优 Bug 4 项 | 见 S2 | KEEP_EXISTING_GRAPH | work:BUG-FAMILY-001/002, work:BUG-INGREDIENT-001, work:BUG-AI-MEAL-001 | 专项台账承载 |
| J2 / J17 | 见 S3 | KEEP_EXISTING_GRAPH | work:J2 / work:J17 | 专项台账承载 |
| K1a / K1b / K1g / K1h / K1i / K1e / K1f | 见 S3 | KEEP_EXISTING_GRAPH / SKIP_HISTORY | work:各 K* | 专项台账承载；K1h/K1f 已完成 SKIP |
| AG-REVIEW | 待复核食材审核闭环 | SKIP_HISTORY | — | ✅ 2026-08-02 三视角会审完成 |
| 编辑页统一 | 见 S3 | KEEP_EXISTING_GRAPH | work:REFACTOR-DISH-001 | 专项台账承载 |
| FAM-AGE / FAM-MEAL | 见 S3 索引 | MIGRATE_EXISTING_ID | work:FAM-AGE / work:FAM-MEAL | **Stable ID 恢复**（P2B-R01/R02） |
| 首页推荐下一餐 v2 / J21 / 走势折线 / 滑块 | 见 S4 | KEEP_EXISTING_GRAPH | work:TODO-RECOMMEND-001, work:J21, work:TODO-NUTRITION-001, work:TODO-FAMILY-001 | 专项台账承载 |
| 食材库扩充阶段2 / 营养表分页 | 见 S5 | KEEP_EXISTING_GRAPH | work:TODO-INGREDIENT-001 / work:TODO-NUTRITION-002 | 专项台账承载 |
| L1 / L2 | 见 S6/S5 | KEEP_EXISTING_GRAPH | work:L1 / work:L2 + work:J22 | L2/J22 双 ID 保留 |
| EER / 食物交换份 / AI S4 | 见 S7/S3 | KEEP_EXISTING_GRAPH | work:TODO-NUTRITION-005/006, work:TODO-AI-MEAL-002 | 专项台账承载 |

**Coverage Summary — S1 `待办索引.md`**

```text
Actionable Items      : 24（索引条目标识，均映射至专项已处置项）
MIGRATE_EXISTING_ID   : 2（FAM-AGE / FAM-MEAL 恢复）
MIGRATE_NEW_ID        : 0
KEEP_EXISTING_GRAPH   : 21
MERGE_NO_ID_DUPLICATE : 0
SKIP_HISTORY          : 1（AG-REVIEW）
DEFER_WITH_REASON     : 0
UNEXPLAINED           : 0
```

---

## 3. S2 `待办_Bug修复.md`

| SOURCE ITEM | TITLE | DISPOSITION | TARGET | STATUS | EVIDENCE |
|---|---|---|---|---|---|
| 高优-1 | 家庭成员设不在场→今日卡消失 | MIGRATE_NEW_ID | work:BUG-FAMILY-001 | backlog | 源 ⬜ |
| 高优-2 | 缺席微调 | MIGRATE_NEW_ID | work:BUG-FAMILY-002 | backlog | 源 ⬜ |
| 高优-3 | 添加食材清空预填被覆盖 | MIGRATE_NEW_ID | work:BUG-INGREDIENT-001 | backlog | 源 ⬜ |
| 高优-4 | K2语音 无授权无反应 | MIGRATE_NEW_ID | work:BUG-AI-MEAL-001 | backlog | 源 ⬜ |
| 中-I3 | 菜名被当单一食材 | MIGRATE_EXISTING_ID | work:I3 | backlog | 源 ⬜ |
| 中-I4 | 复合菜与括号主食遗漏 | MIGRATE_EXISTING_ID | work:I4 | backlog | 源 ⬜ |
| 中-I5 | 连接词切分错误 | MIGRATE_EXISTING_ID | work:I5 | backlog | 源 ⬜ |
| 中-I6 | DeepSeek 返回无效 | MIGRATE_EXISTING_ID | work:I6 | backlog | 源 ⬜ |
| 中-I7 | AI失败静默降级 | MIGRATE_EXISTING_ID | work:I7 | backlog | 源 ⬜；K15 已拆出独立（P2B-R03） |
| 中-I8 | 查看建议结果不随编辑清空 | MIGRATE_EXISTING_ID | work:I8 | backlog | 源 ⬜ |
| 中-J5 | 食历日期范围 | MIGRATE_EXISTING_ID | work:J5 | backlog | 源 ⬜ |
| 归档区 J1/J18/K9/K10/J12 等 | 已修复历史项 | SKIP_HISTORY | — | — | 源 ✅ 无当前引用 |

**Coverage Summary — S2 `待办_Bug修复.md`**

```text
Actionable Items      : 16（含 SKIP_HISTORY 处置项，与蓝图 §13 口径一致）
MIGRATE_EXISTING_ID   : 7（I3-I8, J5）
MIGRATE_NEW_ID        : 4（BUG-FAMILY-001/002, BUG-INGREDIENT-001, BUG-AI-MEAL-001）
KEEP_EXISTING_GRAPH   : 0
MERGE_NO_ID_DUPLICATE : 0
SKIP_HISTORY          : 5（归档区已完成）
DEFER_WITH_REASON     : 0
UNEXPLAINED           : 0
```

---

## 4. S3 `待办_功能算法.md`

| SOURCE ITEM | TITLE | DISPOSITION | TARGET | STATUS | EVIDENCE |
|---|---|---|---|---|---|
| J2 | 食用比例进度条+成员维度 | MIGRATE_EXISTING_ID | work:J2 | backlog | 源 📄 |
| J17 | 一周计划营养线统一 | MIGRATE_EXISTING_ID | work:J17 | backlog | 源 📄 |
| K1a | AI预览页营养展示统一化 | KEEP_EXISTING_GRAPH | work:K1a | verifying | 源 ✅ + BLUEPRINT ACCEPTED，真机 E-K1A-01 pending |
| K1b | 膳食健康评价逐成员化 | KEEP_EXISTING_GRAPH | work:K1b | parked | 源 🔄📄；蓝图 DRAFT·PARKED |
| K1g | 周期记+NDJSON | KEEP_EXISTING_GRAPH | work:K1g | verifying | 源 📄；SESSION 确认 B1-B6 ACCEPTED 真机 pending |
| K1h | 菜品/食材自动添加调研 | SKIP_HISTORY | — | — | 源 ✅（2026-08-08 完成） |
| K1i | 全App AI输出流式 | KEEP_EXISTING_GRAPH | work:K1i | verifying | 源 ⬜→BLUEPRINT ACCEPTED，真机 E-K1I-01/02 pending |
| K1e | 语义转换层 | KEEP_EXISTING_GRAPH | work:K1e | cancelled | 源 ❌废弃；用户裁定废弃 |
| K1f | AI食材别名归一 | SKIP_HISTORY | — | — | 源 ✅（2026-08-01 已实现） |
| 无ID | 添加食材 vs 添加菜品编辑页统一 | MIGRATE_NEW_ID | work:REFACTOR-DISH-001 | backlog | 源 ⬜ |
| K9-K12 | AI推荐深化/降级提示/越用越顺手/定向菜单 | MIGRATE_EXISTING_ID | work:K9/K10/K11/K12 | backlog | 源 ⬜ |
| K13 | 库存随过期计划消耗 | MIGRATE_EXISTING_ID | work:K13 | backlog | 源 ⬜ |
| K14 | 食材来源筛选 | MIGRATE_EXISTING_ID | work:K14 | backlog | 源 ⬜ |
| **K15** | **AI分段解析与可控降级** | **MIGRATE_EXISTING_ID** | **work:K15** | backlog | **恢复独立（P2B-R03），源 ⬜** |
| K1c | 规则引擎日期推算 | KEEP_EXISTING_GRAPH | work:K1c | **in_progress** | 源 🔄；**状态修正（P2B-R06）** |
| K1d | JSON Schema 双端兼容 | KEEP_EXISTING_GRAPH | work:K1d | backlog | 源 ⬜；双文件同一项 |
| J7 | 菜品编辑加"非家庭用餐" | MIGRATE_EXISTING_ID | work:J7 | backlog | 源 ⬜ |
| J14 | 菜名解析自动补调料/主食 | MIGRATE_EXISTING_ID | work:J14 | backlog | 源 ⬜ |
| 无ID | 菜品加食材智能默认剂量 | SKIP_HISTORY | — | — | 待办总览确认已实现（SeasoningDefaults.GROUP_GRAMS，2026-07-22）；专项状态滞后 → 记 STATE_CONFLICT |
| 无ID | 自由搭配引入AI推荐 | MIGRATE_NEW_ID | work:TODO-RECOMMEND-002 | backlog | 源 ⬜ |
| 无ID | AI推荐结合历史餐食学习 | MIGRATE_NEW_ID | work:TODO-RECOMMEND-003 | backlog | 源 ⬜ |
| 无ID | 推演类功能接入AI增强 | DEFER_WITH_REASON | — | — | 跨 F-INGREDIENT/F-NUTRITION/F-HEALTH 泛化能力，ownership 未决 → Conflict FEATURE_OWNERSHIP_UNCERTAIN-03 待 2C |
| 无ID | AI营养补全（云端档） | MIGRATE_NEW_ID | work:TODO-AI-MEAL-004 | backlog | 源 ⬜ |
| 无ID | AI S4 端侧本地模型 | MIGRATE_NEW_ID | work:TODO-AI-MEAL-002 | backlog | 源 ⬜（低优先级，仍可行动） |
| 无ID | 端侧免费OCR·拍营养成分表 | MIGRATE_NEW_ID | work:TODO-INGREDIENT-002 | backlog | 源 ⬜（低优先级，仍可行动） |
| 无ID | AI对话生成菜品/餐食 | KIND_ID_CONVENTION_REQUIRED | — | — | 二期明确计划但无 Stable ID 且必须 kind:feature → 记 Conflict，暂不迁（§31） |
| 无ID | 放开AI推荐限制（自由创菜） | KIND_ID_CONVENTION_REQUIRED | — | — | 用户定"暂不动"；无 Stable ID 且必须 kind:feature → 记 Conflict，暂不迁（§31） |
| 已归档 K1/K4/K7 等 | 历史完成项 | SKIP_HISTORY | — | — | 源 ✅ |

**Coverage Summary — S3 `待办_功能算法.md`**

```text
Actionable Items        : 31
MIGRATE_EXISTING_ID     : 11（J2/J17/K9/K10/K11/K12/K13/K14/K15/J7/J14）
MIGRATE_NEW_ID          : 6（REFACTOR-DISH-001, TODO-RECOMMEND-002, TODO-RECOMMEND-003, TODO-AI-MEAL-002, TODO-AI-MEAL-004, TODO-INGREDIENT-002）
KEEP_EXISTING_GRAPH     : 7（K1a/K1b/K1g/K1i/K1e/K1c/K1d）
MERGE_NO_ID_DUPLICATE   : 0
SKIP_HISTORY            : 4（K1h/K1f/菜品默认剂量/已归档区）
DEFER_WITH_REASON       : 1（推演类接入AI增强）
KIND_ID_CONVENTION_REQUIRED : 2（AI对话生成 / 放开AI推荐限制）
UNEXPLAINED             : 0
```

> 注：K1e 在 Graph 为 cancelled（废弃项），计入 KEEP_EXISTING_GRAPH（明确废弃语义）；SKIP_HISTORY 严格门禁下取消项不算历史完成。

---

## 5. S4 `待办_UI交互.md`

| SOURCE ITEM | TITLE | DISPOSITION | TARGET | STATUS | EVIDENCE |
|---|---|---|---|---|---|
| 无ID | 首页推荐下一餐 v2 | MIGRATE_NEW_ID | work:TODO-RECOMMEND-001 | backlog | 源 ⬜ |
| J21 | "按实际吃了多少"入口可点性 | MIGRATE_EXISTING_ID | work:J21 | backlog | 源 ⬜ |
| 无ID | 营养走势折线三线同显 | MIGRATE_NEW_ID | work:TODO-NUTRITION-001 | backlog | 源 ⬜ |
| 无ID | 家庭成员>4 滑块 | MIGRATE_NEW_ID | work:TODO-FAMILY-001 | backlog | 源 ⬜ |
| **I-Mine** | **"我的"页重新归类** | **MIGRATE_EXISTING_ID** | **work:I-Mine** | backlog | **恢复 Stable ID（§15/§84）** |
| **I-About** | **"关于·已为你开启"突出** | **MIGRATE_EXISTING_ID** | **work:I-About** | backlog | **恢复 Stable ID（§15/§84）** |
| K2 | 整体界面优化 | MIGRATE_EXISTING_ID | work:K2 | backlog | 源 ⬜ |
| J4 | 食历时间轴+按月折叠 | MIGRATE_EXISTING_ID | work:J4 | backlog | 源 📄 |
| J6 | 色系墙"历年"文案 | MIGRATE_EXISTING_ID | work:J6 | backlog | 源 ⬜ |
| J9 | 食材详情忌口/宜口视觉 | MIGRATE_EXISTING_ID | work:J9 | backlog | 源 ⬜ |
| J10 | 多健康档案综合体现 | MIGRATE_EXISTING_ID | work:J10 | backlog | 源 📄 |
| J11 | 食材搜索二级名称 | MIGRATE_EXISTING_ID | work:J11 | backlog | 源 ⬜ |
| J16 | 采购清单入口移页 | MIGRATE_EXISTING_ID | work:J16 | backlog | 源 ⬜ |
| **U1** | **全食材/菜品卡通图** | **MIGRATE_EXISTING_ID** | **work:U1** | backlog | **恢复 Stable ID（§15/§84）** |
| **U2** | **UI持续优化** | **MIGRATE_EXISTING_ID** | **work:U2** | backlog | **恢复 Stable ID（§15/§84）** |
| **U3** | **设置适老版** | **MIGRATE_EXISTING_ID** | **work:U3** | backlog | **恢复 Stable ID（§15/§84）** |
| **U4** | **临时成员卡片** | **MIGRATE_EXISTING_ID** | **work:U4** | backlog | **恢复 Stable ID（§15/§84）** |
| **U5** | **云端模型连接测试** | **MIGRATE_EXISTING_ID** | **work:U5** | backlog | **恢复 Stable ID（§15/§84）** |
| 已归档区 | UI风格苹果化等历史完成 | SKIP_HISTORY | — | — | 源 ✅ |

**Coverage Summary — S4 `待办_UI交互.md`**

```text
Actionable Items        : 19
MIGRATE_EXISTING_ID     : 15（J21/J4/J6/J9/J10/J11/J16/K2 + I-Mine/I-About/U1/U2/U3/U4/U5）
MIGRATE_NEW_ID          : 3（TODO-RECOMMEND-001, TODO-NUTRITION-001, TODO-FAMILY-001）
KEEP_EXISTING_GRAPH     : 0
MERGE_NO_ID_DUPLICATE   : 0
SKIP_HISTORY            : 1（已归档区）
DEFER_WITH_REASON       : 0
UNEXPLAINED             : 0
```

---

## 6. S5 `待办_数据健康.md`

| SOURCE ITEM | TITLE | DISPOSITION | TARGET | STATUS | EVIDENCE |
|---|---|---|---|---|---|
| 无ID | 食材库扩充阶段2（USDA→5000+） | MIGRATE_NEW_ID | work:TODO-INGREDIENT-001 | in_progress | 源 🔄 |
| 无ID | 食材营养表分页查询 | MIGRATE_NEW_ID | work:TODO-NUTRITION-002 | backlog | 源 📄 |
| J13 | 脚本化月度营养交叉对比 | MIGRATE_EXISTING_ID | work:J13 | backlog | 源 ⬜ |
| J20 | 调料"限量"口径 | MIGRATE_EXISTING_ID | work:J20 | backlog | 源 📄 |
| **J22** | **健康状态加脂肪肝** | **MIGRATE_EXISTING_ID** | **work:J22** | backlog | **恢复独立（P2B-R04）** |
| 无ID | 食材补充维生素数据 2b | MIGRATE_NEW_ID | work:TODO-NUTRITION-003 | backlog | 源 🔨（2a 完成 2b 待做） |
| 无ID | 补"青菜"泛称食材 | MIGRATE_NEW_ID | work:TODO-INGREDIENT-003 | backlog | 源 📄 |
| 无ID | AI规则推荐不贴合 MID-1 | MIGRATE_NEW_ID | work:TODO-RECOMMEND-004 | backlog | 源 📄 |
| 无ID | 全库忌口补漏剩余边界 | MIGRATE_NEW_ID | work:TODO-HEALTH-001 | backlog | 源 🔄 |
| 无ID | GI/纤维覆盖率复核 | MIGRATE_NEW_ID | work:TODO-NUTRITION-007 | backlog | 源 🔨 |
| 无ID | 食材营养表体现营养素+属性+冻结左列 | MIGRATE_NEW_ID | work:TODO-NUTRITION-008 | backlog | 源 ⬜（等属性体系落地） |
| 已归档区 | 历史完成项 | SKIP_HISTORY | — | — | 源 ✅ |

**Coverage Summary — S5 `待办_数据健康.md`**

```text
Actionable Items        : 12
MIGRATE_EXISTING_ID     : 3（J13/J20/J22）
MIGRATE_NEW_ID          : 8（TODO-INGREDIENT-001, TODO-INGREDIENT-003, TODO-NUTRITION-002, TODO-NUTRITION-003, TODO-NUTRITION-007, TODO-NUTRITION-008, TODO-RECOMMEND-004, TODO-HEALTH-001）
KEEP_EXISTING_GRAPH     : 0
MERGE_NO_ID_DUPLICATE   : 0
SKIP_HISTORY            : 1（已归档区）
DEFER_WITH_REASON       : 0
UNEXPLAINED             : 0
```

---

## 7. S6 `待办_工程合规.md`

| SOURCE ITEM | TITLE | DISPOSITION | TARGET | STATUS | EVIDENCE |
|---|---|---|---|---|---|
| L1 | 用户协议免责+AI弹窗 | KEEP_EXISTING_GRAPH | work:L1 | verifying | 源 ⬜；BLUEPRINT ACCEPTED，真机 E-L1-01~12 pending |
| L2 | 健康状态加脂肪肝 | KEEP_EXISTING_GRAPH | work:L2 | backlog | 源 ⬜；与 J22 双 ID 保留（P2B-R04） |
| 无ID | 数据规模化性能优化 | MIGRATE_NEW_ID | work:TODO-TOOLS-006 | backlog | 源 ⬜ |
| K1d | JSON Schema 双端兼容 | KEEP_EXISTING_GRAPH | work:K1d | backlog | 源 ⬜；与功能算法双文件同一项 |
| 无ID | P2写入Diff/P3 KMP日志/DI平台化 | MIGRATE_NEW_ID | work:TODO-TOOLS-007 | backlog | 源 ⬜（低优先级） |
| 无ID | iOS shared 编译能力 | MIGRATE_NEW_ID | work:TODO-TOOLS-008 | parked | 源 📌 二期明确暂缓 → parked |
| 已归档区 | 历史完成项 | SKIP_HISTORY | — | — | 源 ✅ |

**Coverage Summary — S6 `待办_工程合规.md`**

```text
Actionable Items        : 7
MIGRATE_EXISTING_ID     : 0
MIGRATE_NEW_ID          : 3（TODO-TOOLS-006/007/008）
KEEP_EXISTING_GRAPH     : 3（L1/L2/K1d）
MERGE_NO_ID_DUPLICATE   : 0
SKIP_HISTORY            : 1（已归档区）
DEFER_WITH_REASON       : 0
UNEXPLAINED             : 0
```

---

## 8. S7 `待办_战略会商.md`

| SOURCE ITEM | TITLE | DISPOSITION | TARGET | STATUS | EVIDENCE |
|---|---|---|---|---|---|
| J3 | 营养快照还是实时 | MIGRATE_EXISTING_ID | work:J3 | backlog | 源 📄 |
| J19 | 规则+数据开源壁垒 | MIGRATE_EXISTING_ID | work:J19 | backlog | 源 📄 |
| **L3** | **全App自动化进阶** | **KEEP_EXISTING_GRAPH** | **work:L3** | backlog | **Primary 修正 F-AI-MEAL→F-TOOLS（P2B-R07）+ FEATURE_SPLIT_CANDIDATE** |
| L4 | 分享链接解析 | MIGRATE_EXISTING_ID | work:L4 | ready | 源 📄 方案已出 |
| 无ID | 餐状态机："计划记了但没吃" | MIGRATE_NEW_ID | work:TODO-MEAL-001 | backlog | 源 📄 |
| 无ID | 周计划"营养线"整体均衡价值 | MIGRATE_NEW_ID | work:TODO-WEEKPLAN-001 | backlog | 源 📄 |
| 无ID | 全功能权威化重审 | MIGRATE_NEW_ID | work:TODO-NUTRITION-009 | backlog | 源 📄（XL 级） |
| 无ID | 生命阶段适配推荐/评估 | MIGRATE_NEW_ID | work:TODO-HEALTH-002 | backlog | 源 📄 |
| 无ID | 推荐正向优化：按健康推有利菜 | MIGRATE_NEW_ID | work:TODO-RECOMMEND-005 | backlog | 源 📄 |
| 无ID | 食物多样性提示 | MIGRATE_NEW_ID | work:TODO-RECOMMEND-006 | backlog | 源 📄 |
| 无ID | 微量营养素评估扩展 | MIGRATE_NEW_ID | work:TODO-NUTRITION-004 | backlog | 源 📄 |
| 无ID | EER个人能量更精细 | MIGRATE_NEW_ID | work:TODO-NUTRITION-005 | backlog | 源 ⬜ |
| 无ID | 食物交换份 | MIGRATE_NEW_ID | work:TODO-NUTRITION-006 | backlog | 源 ⬜ |
| 无ID | S1 自由搭配去留会商 | MIGRATE_NEW_ID | work:TODO-RECOMMEND-007 | backlog | 源 📌 二期明确计划 |
| 无ID | S2 发布渠道探索 | MIGRATE_NEW_ID | work:TODO-TOOLS-009 | backlog | 源 📌 二期明确计划 |
| 无ID | S3 家庭饭量模型深化 | MIGRATE_NEW_ID | work:TODO-FAMILY-005 | backlog | 源 📌 二期明确计划 |
| 无ID | 账号体系深化/AI营养师订阅 | MIGRATE_NEW_ID | work:TODO-TOOLS-010 | backlog | 源 📌 二期明确计划 |
| 无ID | 拍照扫码系/条码录入 | MIGRATE_NEW_ID | work:TODO-TOOLS-011 | backlog | 源 📌 二期明确计划 |
| 已归档区 | 历史完成项 | SKIP_HISTORY | — | — | 源 ✅ |

**Coverage Summary — S7 `待办_战略会商.md`**

```text
Actionable Items        : 19
MIGRATE_EXISTING_ID     : 3（J3/J19/L4）
MIGRATE_NEW_ID          : 14（TODO-MEAL-001, TODO-WEEKPLAN-001, TODO-NUTRITION-009, TODO-NUTRITION-004, TODO-NUTRITION-005, TODO-NUTRITION-006, TODO-HEALTH-002, TODO-RECOMMEND-005, TODO-RECOMMEND-006, TODO-RECOMMEND-007, TODO-FAMILY-005, TODO-TOOLS-009, TODO-TOOLS-010, TODO-TOOLS-011）
KEEP_EXISTING_GRAPH     : 1（L3）
MERGE_NO_ID_DUPLICATE   : 0
SKIP_HISTORY            : 1（已归档区）
DEFER_WITH_REASON       : 0
UNEXPLAINED             : 0
```

---

## 9. S8 `待办总览.md` —— 历史存档（2026-08-01 已拆分，不再更新）

该文件已拆分，其条目全部反映到 S2-S7 专项。此处处置"专项未覆盖但总览独有、且仍属当前可行动"的项：

| SOURCE ITEM | TITLE | DISPOSITION | TARGET | STATUS | EVIDENCE |
|---|---|---|---|---|---|
| L 段 2026-08-01 新增 L1-L4 | 见 S6/S7 | KEEP_EXISTING_GRAPH | work:L1/L2/L3/L4 | — | 专项承载 |
| K 段 K1-K1h | 见 S3 | KEEP_EXISTING_GRAPH | work:各 K* | — | 专项承载 |
| 无ID | 竞品对比+商业化规划 | MIGRATE_NEW_ID | work:TODO-TOOLS-012 | backlog | 总览 📄 方案已出待拍板 |
| 无ID | 参考内容数据驱动+接口下发 | MIGRATE_NEW_ID | work:TODO-TOOLS-013 | backlog | 总览 📄 |
| 无ID | 反查全App落地新准则 | MIGRATE_NEW_ID | work:TODO-TOOLS-014 | backlog | 总览 📄 |
| 无ID | 用户上报菜品/食材 | MIGRATE_NEW_ID | work:TODO-TOOLS-015 | backlog | 总览 ⬜（依赖联网+账号） |
| 无ID | 通知分层可独立开关 | MIGRATE_NEW_ID | work:TODO-TOOLS-016 | backlog | 总览 ⬜ |
| 无ID | 全App家族化 P2-P4 | MIGRATE_NEW_ID | work:TODO-TOOLS-017 | backlog | 总览 🔨（P1 已交付） |
| 无ID | selectionMode 完整两入口拆分 | MIGRATE_NEW_ID | work:TODO-TOOLS-018 | backlog | 总览 🔨 |
| 无ID | 报告模块二期 | MIGRATE_NEW_ID | work:TODO-NUTRITION-010 | backlog | 总览 🔨 |
| 无ID | 首页营养分配可视化 P4 | MIGRATE_NEW_ID | work:TODO-NUTRITION-011 | backlog | 总览 🔨 |
| 无ID | 食材输入智能推演 scenario1 | MIGRATE_NEW_ID | work:TODO-INGREDIENT-004 | backlog | 总览 ⬜ |
| 其余全部 | 已由专项承载（S2-S7） | KEEP_EXISTING_GRAPH / SKIP_HISTORY | — | — | 专项承载（不计入本表独立 disposition） |

> 注：369道菜/TODO-DISH-001、Sync事务/TODO-SYNC-001、N+1/TODO-TIMELINE-001、pantry重算/TODO-PANTRY-001、快速记餐/TODO-MEAL-002 均属 **S11 UX 审计** 独有，已计入 S11，不在本表重复。

**Coverage Summary — S8 `待办总览.md`**

```text
Actionable Items        : 10（总览独有且仍可行动；其余由专项承载）
MIGRATE_EXISTING_ID     : 0
MIGRATE_NEW_ID          : 10
KEEP_EXISTING_GRAPH     : 0（映射至专项已迁项，不计入本表独立 disposition）
MERGE_NO_ID_DUPLICATE   : 0
SKIP_HISTORY            : 0
DEFER_WITH_REASON       : 0
UNEXPLAINED             : 0
```

---

## 10. S9 `待办执行分组_2026-08-04.md` —— 执行顺序分组，非独立台账

全部条目引用 S2-S7 专项待办（G0-G4），无独立 actionable item。处置：

```text
Actionable Items      : 0（纯执行顺序引用，非台账）
UNEXPLAINED           : 0
```

---

## 11. S10 `工程优化待办.md`（P0-P5）

| SOURCE ITEM | TITLE | DISPOSITION | TARGET | STATUS | EVIDENCE |
|---|---|---|---|---|---|
| P0 | 存储权限合规迁移 | SKIP_HISTORY | — | — | 已完成（2026-07-09 真机重装待验归验证域） |
| P1 | Shared IO Dispatcher | SKIP_HISTORY | — | — | 已完成（2026-07-08） |
| P2 | SQLDelight 写入Diff | MIGRATE_NEW_ID | work:TODO-TOOLS-007 | backlog | 与 S6 低优项合并 |
| P3 | KMP 日志 | MIGRATE_NEW_ID | work:TODO-TOOLS-007 | backlog | 合并 |
| P4 | Koin 模块解耦 | MIGRATE_NEW_ID | work:TODO-TOOLS-007 | backlog | 合并 |
| P5 | Shared ViewModel 长期化 | DEFER_WITH_REASON | — | — | 等 Android UI 稳定/平台消费者明确后再评估；反过度设计红线 |

**Coverage Summary — S10 `工程优化待办.md`**

```text
Actionable Items        : 6
MIGRATE_EXISTING_ID     : 0
MIGRATE_NEW_ID          : 3（TODO-TOOLS-007 聚合 P2/P3/P4）
KEEP_EXISTING_GRAPH     : 0
MERGE_NO_ID_DUPLICATE   : 0
SKIP_HISTORY            : 2（P0/P1 已完成）
DEFER_WITH_REASON       : 1（P5 Shared VM）
UNEXPLAINED             : 0
```

---

## 12. S11 `UX深挖审计与待办.md`（2026-07-14 审计）

| SOURCE ITEM | TITLE | DISPOSITION | TARGET | STATUS | EVIDENCE |
|---|---|---|---|---|---|
| Top1/C1 | AppTopBar 统一顶栏 | KEEP_EXISTING_GRAPH | work:U2 | backlog | 属"UI持续优化"聚合 |
| Top2/H1 | 餐次时间默认值 | SKIP_HISTORY | — | — | ✅ 已做 |
| Top3/C2 | PlainCard 白卡 | KEEP_EXISTING_GRAPH | work:U2 | backlog | 聚合 |
| Top4/C3 | InsetDivider | KEEP_EXISTING_GRAPH | work:U2 | backlog | 聚合 |
| Top5/C4 | ActionSheet 统一 | KEEP_EXISTING_GRAPH | work:U2 | backlog | 聚合 |
| Top6/H3 | 多选已选反馈 | KEEP_EXISTING_GRAPH | work:U2 | backlog | 聚合 |
| Top7/H6 | 长文案精简 | KEEP_EXISTING_GRAPH | work:U2 | backlog | 聚合 |
| Top8/C6 | navigationBarsPadding | KEEP_EXISTING_GRAPH | work:U2 | backlog | 聚合 |
| Top9/C8 | EmptyState 统一 | KEEP_EXISTING_GRAPH | work:U2 | backlog | 聚合 |
| H2 | 新建菜品必填层级 | KEEP_EXISTING_GRAPH | work:REFACTOR-DISH-001 | backlog | 聚合 |
| H4 | 删除 Snackbar Long | KEEP_EXISTING_GRAPH | work:U2 | backlog | 聚合 |
| H5 | 餐次下拉/时间挤压 | KEEP_EXISTING_GRAPH | work:U2 | backlog | 聚合 |
| H7 | 首页快速记一餐 | MIGRATE_NEW_ID | work:TODO-MEAL-002 | backlog | 独立可行动 |
| 369道菜 quantity/steps | 数据补齐 | MIGRATE_NEW_ID | work:TODO-DISH-001 | backlog | 独立可行动 |
| SyncRepository import 事务 | 数据完整性 | MIGRATE_NEW_ID | work:TODO-SYNC-001 | backlog | 独立可行动 |
| observeTimelineCards N+1 | 性能 | MIGRATE_NEW_ID | work:TODO-TIMELINE-001 | backlog | 独立可行动 |
| pantryCardFlags 重算 | 性能 | MIGRATE_NEW_ID | work:TODO-PANTRY-001 | backlog | 独立可行动 |
| 营养覆盖 103/440 | 数据扩充 | KEEP_EXISTING_GRAPH | work:TODO-INGREDIENT-001 | in_progress | 属食材库扩充范畴 |
| 其余视觉/代码细节 | UI/质量打磨 | KEEP_EXISTING_GRAPH | work:U2 / work:REFACTOR-DISH-001 | backlog | 聚合至"UI持续优化" |

**Coverage Summary — S11 `UX深挖审计与待办.md`**

```text
Actionable Items      : 19
MIGRATE_EXISTING_ID   : 0
MIGRATE_NEW_ID        : 5（TODO-MEAL-002, TODO-DISH-001, TODO-SYNC-001, TODO-TIMELINE-001, TODO-PANTRY-001）
KEEP_EXISTING_GRAPH   : 13（聚合至 U2 / REFACTOR-DISH-001 / TODO-INGREDIENT-001）
MERGE_NO_ID_DUPLICATE : 0
SKIP_HISTORY          : 1（餐次时间默认值已做）
DEFER_WITH_REASON     : 0
UNEXPLAINED           : 0
```

---

## 13. 全局 Coverage Summary

```text
TOTAL ACTIONABLE ITEMS        : 139
TOTAL DISPOSITIONED ITEMS     : 139
UNEXPLAINED ITEMS             : 0

按 Disposition（S2-S11 累加，S1 索引层为发现导航不重复计；S9 纯执行顺序引用 0 项）：
MIGRATE_EXISTING_ID           : 39
MIGRATE_NEW_ID                : 56
KEEP_EXISTING_GRAPH           : 24
MERGE_NO_ID_DUPLICATE         : 0
SKIP_HISTORY                  : 16
DEFER_WITH_REASON             : 2
KIND_ID_CONVENTION_REQUIRED   : 2
```

> 逐源明细（格式 = Actionable / MIG_EXIST / MIG_NEW / KEEP / MERGE / SKIP / DEFER / KIND）：
> S2(16)=7/4/0/0/5/0/0 · S3(31)=11/6/7/0/4/1/2 · S4(19)=15/3/0/0/1/0/0
> S5(12)=3/8/0/0/1/0/0 · S6(7)=0/3/3/0/1/0/0 · S7(19)=3/14/1/0/1/0/0
> S8(10)=0/10/0/0/0/0/0 · S10(6)=0/3/0/0/2/1/0 · S11(19)=0/5/13/0/1/0/0
> **门禁：TOTAL ACTIONABLE = TOTAL DISPOSITIONED ✓，UNEXPLAINED = 0 ✓。**
