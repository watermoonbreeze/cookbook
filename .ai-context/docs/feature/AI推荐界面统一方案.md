# AI 推荐三档界面家族化统一方案（周计划风格对齐·会商产出）

> [AI生成] 2026-07-21。用户提"AI 推荐周计划界面风格需与另两个统一·会商讨论"→`apple_ux_designer` 门禁产出。
> **属方案·待编码**（P1 纯风格可直接做·P2 随营养线）。沉淀目标 `苹果风格UI设计方案.md §9.35`。

## 摸底：AI 推荐有几档
`AiRecommendScreen` 单容器·顶部实心 `SegmentedControl` 分 **3 档**：库存推荐 / 随机推荐（同套 `DishRow`/`SuggestionGroupCard`）/ 周期计划（嵌 `AiPlanScreen.AiPlanBody`）。`FreePairingScreen` 是独立路由页·不在三档内。用户说的"另两档"= 库存+随机；**周计划=风格跑偏的第三档**。

## 周计划"不统一"的 7 处（vs 另两档基准）
| # | 差异 | 基准 | 周计划现状 | 病 |
|---|---|---|---|---|
| D1 | 逐日卡底色 | `Surface(surface)` 白卡 | `DayCard`=`surfaceVariant` 灰卡 | **底色选错**·菜名要硬提亮才看清 |
| D2 | 卡内距 | 应 16dp | 14dp | 历史遗留(两档一并对齐) |
| D3 | 卡间距 | Spacer(10) | vertical padding 6 | 节奏不一 |
| D4 | **控件区** | 高频餐次直出+低频进「筛选」弹层(§9.31) | 天数/Slider/人数/生成/风格 **全平铺**5组同屏 | **最刺眼**·没做高频直出低频折叠 |
| D6 | 标注 | 纯文字标红/灰字 | `tertiaryContainer` 胶囊徽标 | 标注语言不统一 |
| D7 | 图标 | 去装饰 emoji·Icons.Outlined | `🛒采购`/`⚠缺` emoji | 与家族化不符 |

**一句话**：卡片灰底(D1)+控件没折叠(D4)+标注各说各话(D6/D7)。骨架没问题，皮肤和分层没跟上。

## 统一基调（向《App操作基调》八条对齐·收敛4条）
1. **卡片单一白底**：结果卡统一 `Surface(surface)`+`shapes.medium`(12)+卡内16dp。
2. **卡间距统一 Spacer(10)**。
3. **高频直出/低频折叠(§9.31)**：周计划控件照另两档收敛。
4. **标注语言统一**：全 `Icons.Outlined`+labelSmall·去 emoji·纯文字浅色一套。

## 周计划改造清单（P1·精确到组件·守崩溃红线）
- **R1 DayCard 白卡化**：`surfaceVariant`→`surface`·卡内14→16·卡外 vertical padding→统一 Spacer(10)·卡头"第N天"色 `primary`→`onSurface`(primary 只给可交互)。
- **R2 控件高频直出/低频折叠(核心)**：高频=天数 chip+Slider+「生成」直出；低频=**人数 MiniStepper+风格 SegmentedControl+说明** 收进「计划设置」`ModalBottomSheet`（与另两档「筛选」弹层同构·有非默认值加 ● 圆点）。**方案A 推荐**(逐像素同构)。
- **R3** 季节/健康提示条下移到概览卡之后(让出黄金位)。
- **R4** 空态改统一 `EmptyState`("还没安排这一周"+营养线文案"排上几天就能看到搭得均不均衡")。
- **R5** loading/error 对齐 `LoadingBlock`+重试按钮。
- **R6** 标注去 emoji·"规则补充"胶囊降 labelSmall 灰字·"采购/缺"纯文字(error 色保留)。
- 🔴**崩溃红线**：无 coverStyle 变体·`DayCard` 内无 `return@Column/Row`·灰显用 `Modifier.alpha` 非提前 return。

## 为「一周营养搭配」概览卡预留（一次设计到位·避免返工）
AiPlanBody LazyColumn item 结构（生成后·自上而下）：
```
item0 控件区(天数chip+Slider+生成+设置●)   ← R2 折叠后
item1 【一周营养搭配 概览卡】★营养线预留槽   ← InsetGroup(title) 承载·总卡
item2 生成上下文条(季节/健康档案)          ← R3 迁移
item3+ DayCard×N(白卡化·内容分卡)
item末 免责句
```
- **总—分层级**：概览卡=摘要层(`InsetGroup` 卡外标题·总卡·可略强调)、逐日卡=明细层(内容白卡·卡内标题)。同族但有层级，别让"总"淹没在"分"。
- 色阶复用 §9.25 `Dot`+`ExtendedColors`·**不上红·不铺整行底色**(营养线§七红线)。
- 营养线落地只需填 item1 槽·不动其余布局。

## 分批
- **P1 纯风格统一(先做·免真机快赢)**：R1白卡化+R2控件折叠(方案A)+R3+R6。无算法依赖·纯 UI→本稿即可编码→`google_quality_engineer` 审 UI diff。
- **P1.5 状态统一**：R4 空态+R5 loading/error。随 P1。
- **P2 随营养线**：概览卡(item1)填充·依赖 `WeeklyNutritionLineAggregator` 先落地→营养线§十门禁(Apple-UX 色阶已预埋/软件行为师 T1/copywriter)。
- **视觉师**：P1 不需(机械对齐已确立 token)；P2 概览卡色阶需 `apple_visual_designer` 定稿(色点/胶囊/深色α)。

## 沉淀 §9.35（编码落地后回写 `苹果风格UI设计方案.md`）
AI 推荐三档共用容器+SegmentedControl·结果卡统一白卡(surface/medium/16)·卡间Spacer(10)·标注纯文字浅色labelSmall+Icons.Outlined去emoji·周计划控件遵§9.31高频直出低频折叠·DayCard内容白卡+概览卡InsetGroup总卡(总分层级)·营养线色点复用§9.25不上红·静态卡头onSurface·崩溃红线(无coverStyle/无return@Column/alpha灰显)。

## 关键文件
- 改造主战场 `androidApp/.../ui/ai/AiPlanScreen.kt`(`AiPlanBody`+`DayCard`)
- 基准参照 `androidApp/.../ui/ai/AiRecommendScreen.kt`(`RecommendControls`/`RecommendFilterSheet`/`SuggestionGroupCard`/`DishRow`)
- 复用件 `InsetGroup`/`SegmentedControl`/`MiniStepper`/`CapsuleButton`/`EmptyState`/`MoreOptionsHeader`
- 关联 `周计划营养线方案.md`(P2依据)、`App操作基调_设计系统.md`(基调)
