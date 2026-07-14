# Cookbook 苹果风格 UI 设计方案（若 Apple 来设计这款 App）

> [AI生成] 2026-07-14。以 Apple 设计师视角，参考 iOS Human Interface Guidelines 与苹果自家 App（健康 Health、健身 Fitness、提醒事项 Reminders、备忘录 Notes、天气、Apple Store）为本应用（家庭记菜 + 慢病饮食参考）出一套完整视觉与交互方案。
> 现状：Compose + Material3 **1.1.2**、明暗双主题、暖杏色系（primary `#D9A882`）。本方案**评估先行、改造分批**，不一次性重做。落地映射到现有 `theme/Color.kt`、`Theme.kt`（Typography/Shapes）、`ExtendedColors.kt` 与组件层。

---

## 一、设计理念（Apple 三原则落到本 App）

1. **清晰 Clarity**：内容第一、控件让位。大标题、充足留白、清晰的字号层级；每屏一个明确主操作。
2. **遵从 Deference**：界面为内容服务——弱化边框与阴影，用**分组内嵌列表（grouped inset list）**和中性背景托起"菜/食材/餐"这些内容本身；**单一强调色**贯穿所有可交互元素，其余皆中性。
3. **深度 Depth**：用层级（背景灰 → 白卡 → 弹出 sheet）和轻材质表达空间，而非重投影。转场自然、有物理感。

**本 App 的气质定位**：温暖、可信赖、克制。像"健康 App 的严谨 + 备忘录的轻松"。慢病信息要**冷静专业**（不制造焦虑），日常记菜要**轻快低负担**。

---

## 二、色彩系统

Apple 的做法：**一个强调色（tint）贯穿全局可交互元素**，骨架由**中性灰阶**搭建，语义色（红/黄/绿）**只用于状态**、克制出现。当前的"暖杏"偏土、饱和度和对比都偏弱；方案在保留"暖·食欲"基因的前提下，提纯为更精致的赤陶橘 + 中性暖灰。

### 2.1 主色调（Primary / 品牌强调色 Tint）
**赤陶橘 Terracotta**——食欲感、温暖、区别于满大街的"外卖红"。全 App **唯一** tint：按钮/选中态/开关/链接/FAB/进度/焦点。

| 角色 | Light | Dark | 用途 |
|---|---|---|---|
| accent（主强调） | `#DC6E3C` | `#F0895A` | 主操作、选中、链接、tint |
| accent-pressed | `#C25E30` | `#E0794A` | 按压态 |
| accent-container（浅底） | `#FBEDE4` | `#4A2E20` | 选中项浅底、chip 选中底 |
| on-accent | `#FFFFFF` | `#2A1710` | 强调色上的文字 |

> Material 映射：`primary=accent`、`onPrimary=on-accent`、`primaryContainer=accent-container`。

### 2.2 二级色调（Secondary / 中性结构色）
**不是第二个鲜艳色**，而是一套"暖中性灰"，负责背景/卡片/分隔/次要文字——这是 Apple 分组列表的骨架。

| 角色 | Light | Dark | 用途 |
|---|---|---|---|
| bg-grouped（分组页背景） | `#F5F2EE` | `#161311` | 页面底（灰） |
| surface（卡片/列表底） | `#FFFFFF` | `#242019` | 白卡/列表项 |
| surface-elevated（弹层） | `#FFFFFF` | `#2C2721` | sheet/dialog |
| separator（分隔线，1px） | `#E7E1D9` | `#3A342D` | 内嵌分隔（细） |
| fill-secondary（次级填充） | `#EFEAE3` | `#2E2A24` | segmented 轨道、tag 底 |

### 2.3 文字色阶（Label hierarchy，仿 iOS label/secondary/tertiary）
| 角色 | Light | Dark | 用途 |
|---|---|---|---|
| label（主文字） | `#1C1A17` | `#F6F1EA` | 标题/正文 |
| label-secondary（次要） | `#8A8075` | `#B4ABA0` | 说明、副文本 |
| label-tertiary（三级/占位） | `#B4ABA0` | `#7E766C` | 占位符、禁用 |

### 2.4 点缀色（Accent / 语义状态色）——**只用于状态，克制**
| 语义 | Light | Dark | 用途（本 App） |
|---|---|---|---|
| 成功·健康·利调养 绿 | `#5C9A6A` | `#84BE91` | ✓利于调养、健康达标、库存充足 |
| 提醒·限量 琥珀 | `#E0A23C` | `#EDB65C` | ⚠限量、库存不足、临期 |
| 忌口·危险 红 | `#D14E3B` | `#F0836F` | ⛔忌口标红、删除、错误 |
| 收藏 金橘 | 复用 accent 或 `#E7A93C` | `#F0C062` | ⭐收藏置顶 |

> 语义色**不做大面积底色**，仅用于小面积文字/图标/角标/细条，避免慢病信息制造焦虑。忌口菜"排最后 + 红字警示"而非红底吓人（延续现有正确做法）。

### 2.5 深色模式
非纯黑：用**暖近黑** `#161311` 作分组背景，卡片 `#242019` 抬升，强调色**提亮**到 `#F0895A` 保证在暗底可读。所有对比满足 WCAG AA（正文 ≥4.5:1、大字 ≥3:1）。

---

## 三、字体系统（Type Scale）

Apple 的关键差异：**正文用 17pt、层级拉开、字重克制（多用 Regular/Semibold 两档）**。当前正文 14 偏小、层级偏平。方案对齐 iOS Dynamic Type 的语义级别（映射到 Compose `Typography`）。

字体族：Android 无 SF Pro，用**系统默认**（拉丁 Roboto + 中文思源/苹方由系统匹配）即可，不折腾自带字体；靠**字号 + 字重 + 行高**建立苹果式层级。行高统一约 **1.3×**，字距默认（大标题可 −0.2sp 收紧）。

| iOS 语义 | 字号/字重 | Compose 角色 | 用途 |
|---|---|---|---|
| Large Title | 34 / Bold | `displaySmall` | 页面大标题（滚动折叠） |
| Title 1 | 28 / Bold | `headlineMedium` | 次级大标题/空态标题 |
| Title 2 | 22 / Bold | `titleLarge` | 区块大标题 |
| Title 3 | 20 / Semibold | `titleMedium` | 卡片标题、弹层标题 |
| Headline | 17 / Semibold | `titleSmall` | **列表项主标题**（菜名/食材名） |
| Body | 17 / Regular | `bodyLarge` | **正文主号** |
| Subhead | 15 / Regular | `bodyMedium` | 副文本、说明 |
| Button | 17 / Semibold | `labelLarge` | 按钮文字 |
| Footnote | 13 / Regular | `labelMedium` | 辅助信息、时间戳 |
| Caption | 12 / Regular | `labelSmall` | 角标、最小字 |

> 相比现状：正文 14→**17**、列表标题 16→**17 Semibold**、大标题 20→**34（大标题页）**。视觉更"苹果"、更易读，尤其利于家庭中老年用户。

---

## 四、形状 · 间距 · 层次

### 4.1 圆角（Corner Radius）
苹果偏"连续圆角、适中"。Compose 用 `RoundedCornerShape`（连续曲率 squircle 无法精确复刻，取值略大以近似柔和）。

| 元素 | 圆角 | Compose Shapes |
|---|---|---|
| 小控件/tag | 8 | `small` |
| 卡片/列表分组/输入框 | 12 | `medium` |
| 弹层 sheet/大卡 | 16–20 | `large`/`extraLarge` |
| 主按钮 | **胶囊**（高度一半）| 单独 CapsuleShape |
| 头像/食材缩略图 | 12（圆角方）或圆形 | — |

### 4.2 间距（8pt 网格）
`4 / 8 / 12 / 16 / 20 / 24 / 32`。
- 屏边距：**16**（内容），分组列表左内嵌分隔从文字起始对齐。
- 卡片内 padding：**16**；列表项竖直 padding：**12–14**（触达 ≥44pt/约 48dp）。
- 区块之间：**24–32**（留白是苹果的呼吸感来源）。
- 分组标题到卡片：8；卡片到下一区块：24。

### 4.3 层次（Elevation / Material）
- 少用投影。卡片=白底 + **1px 分隔/极浅阴影**（`tonalElevation` 0–1dp）。
- 弹层/底部 sheet：轻阴影 + 顶部 grabber 小横条。
- 顶栏滚动时由透明→加浅色/毛玻璃材质（`Surface` 微 elevation），大标题折叠为小标题。

---

## 五、组件规范（iOS 化）

1. **分组内嵌列表 Grouped Inset List（核心）**：内容装进白色圆角卡（`medium`），置于分组灰背景上；卡内多项用**内嵌细分隔线**（左侧与文字对齐、不通栏）。用于：我的/设置、食材详情各区、菜品信息、一周计划的每天。——这是把整个 App"苹果化"的最大单点杠杆。
2. **大标题导航 Large Title**：主页面（今天吃什么/菜品/食材/我的）用大标题，下滑折叠为居中小标题 + 毛玻璃底。右上放**单个**主操作（+ 或 搜索）。
3. **底部 Tab Bar**：图标 + 短标签，选中 tint、未选中中性灰；半透明材质底。图标语言统一（线性、2pt 粗细、圆角端点，近似 SF Symbols）。
4. **Segmented Control（分段控件）替代部分 Material Chips**：互斥选择（餐次全部/早/中/晚、推荐 库存/随机/周期、去重周期 一周/二周/三周/四周、菜品 最近/喜爱/菜系/家庭）→ 用**胶囊轨道 + 滑动选中块**的 segmented control，比一排 chip 更 iOS、更省空间、选中态更清晰。
5. **按钮**：
   - 主 CTA（保存/确定）：**胶囊填充**（accent 底、白字），一屏一个、常驻底部或右上。
   - 次操作：**纯文字**（accent 字、无底）。
   - 危险：**红色文字**；破坏性确认走**底部 Action Sheet**（"删除"红 + "取消"）。
6. **输入 · 表单**：分组卡内的 inset 字段，左标签/右值或上标签下输入；聚焦时 tint 描边。可选字段折叠进"更多（可选）"（已落地 A7，延续）。
7. **底部 Sheet 弹层**：选择器（配料组/步骤模板/收藏组合/餐次时间）统一用**底部 sheet + grabber**，圆角顶、可下滑关闭；标题栏右侧放主操作（如"+添加"）。
8. **Toast/Snackbar**：短、克制；破坏性操作给**撤销 Snackbar**（已落地 A6，延续）。成功给轻 Toast（已落地 A4）。
9. **空态 Empty State**：居中大图标（SF-Symbol 风）+ 一句话 + 一个主操作，大量留白、不焦虑。
10. **列表滑动操作 Swipe Actions**：列表项左划露出**删除（红）**、右划露出**收藏（金）/复制**——替代部分长按菜单，更 iOS、更快。

---

## 六、界面操作逻辑（交互）

- **内容优先、层级清楚**：每屏一个主任务、一个主 CTA；chrome（工具栏按钮）最少化。
- **渐进披露**：高频字段直出、低频折叠（新建菜品已做）。
- **手势优先**：全局右滑返回；列表左右滑操作；底部 sheet 下滑关闭。
- **即时反馈 + 可撤销**：破坏性操作用撤销而非二次确认弹窗（家庭高频误操作救得回）。
- **合理默认、少输入**：餐次默认当前时间（已做）、入库默认 1 份（已做）、模板/配料组/收藏组合一键复用。
- **一致性**：同类互斥选择统一用 segmented；同类弹层统一底部 sheet + 右上"+添加"；同类术语统一。

### 关键页面重构要点（分批落地时逐屏）
- **首页「今天吃什么」**：大标题 + 分组区块（今日餐 / 计划 / 一周计划入口 / AI 推荐入口 / 常做菜），卡片化、留白拉开。
- **菜品列表**：大标题 + 搜索常驻；Tab 改 segmented；列表项 17 Semibold 菜名 + 缩略图圆角方；左划删除、右划收藏。
- **食材页**：主/次维度用 segmented；分组内嵌列表；库存/常用置顶。
- **AI 推荐**：模式与去重周期用 segmented；结果卡片化；"换一换"常驻顶部（已做）；忌口红字、最近浅灰（已做）。
- **添加餐食**：餐次 segmented、时间 inline、菜品网格卡片；主 CTA 胶囊常驻底部。
- **一周计划**：每天用分组卡（周几 + today 高亮 + 餐次内嵌行），空日"安排"行。
- **我的/设置**：标准 iOS 分组设置列表（分区标题 + 白卡 + inset 分隔 + 右 chevron）。

---

## 七、落地路线（分阶段、低风险优先）

**Phase 0 · 设计 Token（最高性价比，改动集中在 theme/）**
- 更新 `Color.kt`（赤陶橘 + 暖中性 + 语义色，明暗两套）、`Theme.kt` 的 `Typography`（17 正文体系）与 `Shapes`、`ExtendedColors`（语义色对齐）。
- 加 `CapsuleShape`、间距/圆角常量表（`Dimens`）。
- **一次改 token，全 App 视觉即刻提升**，风险低（不动业务逻辑）。

**Phase 1 · 组件库（可复用 Composable）**
- `InsetGroupedList` / `SettingsGroup`（分组内嵌列表）、`SegmentedControl`、`CapsuleButton`、`AppBottomSheet`（grabber）、`SwipeableRow`、`EmptyState` 统一化。

**Phase 2 · 逐屏迁移**（按高频→低频）：首页 → 菜品 → 食材 → AI 推荐 → 添加餐食 → 一周计划 → 我的/设置。每屏独立提交、可回滚。

**Phase 3 · 微交互**：大标题折叠、滑动操作、轻触感（haptics）、转场动效打磨。

**Phase 4 · 深色模式细校 + 无障碍**：对比度、Dynamic Type 缩放、VoiceOver/TalkBack 语义。

> 每阶段构建 + 单测 + UX 体验测试通过再进下一阶段；token 阶段先做，用户即可直观看到"苹果味"。

---

## 八、与现有的衔接（务实约束）
- Material3 **1.1.2** 无部分新 API（如 `SelectableDates`）；segmented 可用 M3 `SegmentedButton`（1.1 起可用）或自绘胶囊。
- 保留明暗双主题与 `ThemeMode`；保留 `ExtendedColors` 语义色机制。
- 大标题：M3 `LargeTopAppBar` + `TopAppBarScrollBehavior` 可直接实现折叠。
- 不引入新字体资源（用系统字），降体积与兼容风险。
- 健康数据展示遵守免责红线；语义色克制、不制造焦虑。
