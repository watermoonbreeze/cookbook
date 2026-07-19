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

---

## 九、交互模式库（已确立的可复用规范，后续按此做）

> [AI生成 2026-07] 本节沉淀已落地并确认的具体交互模式，是"苹果三原则"落到本 App 的**可复用规范**。新功能优先复用这里的模式，保持全 App 一致。

### 9.1 选择/多选：勾选圈（iOS Photos/文件 式）
- 可选列表项(食材/菜品卡)在**选择模式**下右上角显**勾选圈**：未选=空心圈(细描边)，已选=实心主色圈+✓。
- **点勾选圈 = 直接选/取消**(连加多项一键勾)；**点卡片主体 = 看详情**(详情里也有"选择"按钮)。两条路都显性、detail 永不隐藏。
- 禁用"点卡=强制开详情再点加入"(每项3步)、也禁用"点=选、详情藏进长按"(长按不可发现)。

### 9.2 搜索结果点击 = 直接开详情（不做网格定位跳转）
- 搜索下拉结果点击 → **直接打开详情弹层**(顶部显**分类路径**面包屑 `常规 › 蔬菜类 › 叶菜`)，不靠"定位到网格并高亮"。
- 原因：定位跳转对**未加载到当前页**的项会"点了没反应"，对**自建/家庭**项会跳错到常规 Tab。跨屏搜索(如首页→食材页)同样传"直接开详情"而非 jump。

### 9.3 详情/底部弹层
- 底部详情弹层占屏 **~0.85**(内容多时少滚动)；顶部圆角 20dp；外部空白点击关闭。
- 组件"能力显隐"由**回调是否传入**决定(如"存为菜品""编辑"按钮)，别在组件内用 mode 布尔硬编码。

### 9.4 角标 / 徽章
- 用**贴角小圆**(CircleShape+单字，如主食"主")而非角标三角/大色块——苹果式克制。字号压到刚好嵌进圆(≈9sp)。

### 9.5 系统栏（状态栏/导航栏）与界面浑然一体
- 系统栏色**跟随 Compose 主题背景**：主题内 `SideEffect{ window.statusBarColor=navigationBarColor=colorScheme.background.toArgb() }`。
- 只设透明+edge-to-edge **不够**(会露 android 默认 windowBackground，app深色但系统浅色时割裂)。`isNavigationBarContrastEnforced=false` 关 scrim。

### 9.6 空态：永远给"下一步"
- 任何列表/网格空时给 `emoji + 一行主文案 + 一行操作引导`(按场景分文案)，禁纯空白(让人以为出错)。

### 9.7 悬浮可拖动按钮
- 次要工具动作(横竖屏切换/回今天)用**右下角悬浮小 FAB**，可拖到任意角、限制不出屏；样式随状态变(如横屏态主色实心+图标旋转)。

### 9.8 低频区折叠
- 低频输入(备注/详情说明/营养素录入)默认**收起为"+ X"文字按钮**，点开才展开；有内容则默认展开。减纵向噪音。

### 9.9 破坏性 / 未保存保护
- 编辑表单有未保存改动时返回 → 弹"放弃未保存的更改?"(放弃=红字/继续编辑)；无改动直接返回。BackHandler + 顶栏返回统一走守卫。
- **撤销优于确认**：移除类操作用 Snackbar 撤销，别用硬确认弹窗(破坏性且高频误触场景尤其)。

### 9.10 主行动 CTA
- "查看型"终点页(菜品详情)底部给**胶囊主 CTA**(如"记这道菜")→ 带入下一步流程预填，让页面=行动终点、非死胡同。

### 9.11 滚动收起 / 行高亮 / 智能默认
- 长表格上划时**次要栏(筛选chip/统计)收起**、表头固定、下划再现——腾内容空间。
- 宽表横滑时**选中整行高亮**(锁定视线)。
- 默认值智能化：按当前钟点选默认餐次、按大类预选默认单位、按名预选营养大类——少一步操作。

> [AI生成 2026-07-16] 以下 9.12–9.17 由 Apple-UX 审阅全库后新增，配套 4 个复用组件(`AppSnackbarHost`/`UnsavedGuard`/`EmptyState`+action/`FormBottomBar`)。**复用对照与 B 批映射见 `交互组件复用指南.md`**。

### 9.12 保存/结果反馈：Snackbar 优先、Toast 仅纯告知
- 全 App **单一 Snackbar 宿主**挂在 `MainScaffold`，各屏经统一封装调用，禁每屏各建 `SnackbarHostState`。
- **可逆破坏操作**(删整天/移除菜/删自建项)→ Snackbar + `actionLabel="撤销"`，`ActionPerformed` 时还原(须先做成软删)。
- **日常成功**("已保存")→ Snackbar 轻提示；**Toast 只留纯告知无跟进项**("已是最新""文件无效")。
- 禁"确定删除…不可撤销"硬确认 AlertDialog 用于可逆删除——**撤销优于确认**。

### 9.13 表单底部 CTA 与"保存并继续"
- 表单主 CTA 用胶囊 `CapsuleButton`、一屏一个、底部常驻(`navigationBarsPadding`+`WindowInsets(0)` 防双下边距)；次操作纯文字。用 `FormBottomBar` 统一摆位。
- 连续录入(建材/建菜)给次 CTA **"保存并继续"**：存后**清空表单+复位默认+留本页+聚焦首输入**并 Snackbar"已保存「X」，继续添加"；主 CTA"保存"仍是存并返回。主按钮永远是"保存/完成"，不是"继续"。

### 9.14 就地数量增减一律用 MiniStepper
- 份数/克数/人数的就地增减统一 `MiniStepper`，禁裸 `+/−`。带单位显示；大跨度(克数)传 `onValueClick` 让中间值可点直接输入；边界用 `minusEnabled/plusEnabled` 锁(份数到 1 禁减)。

### 9.15 大标题页 vs 普通顶栏页
- **从底部 Tab 直达的内容落地页**(首页/菜品/食材/我的)用 `LargeTopAppBar`(下滑折叠、右上单主操作+搜索整行)；**从别处 push 进入、带返回的二级/详情/表单页**用 `AppTopBar`。判据：Tab 落地→大标题，带返回→AppTopBar。
- **大标题折叠燃料**(B-7 落地)：`LargeTopAppBar` 折叠靠 `nestedScroll(scrollBehavior)` 由滚动内容驱动——把搜索行/Tab/计数/筛选一律**下沉进滚动列表头部作 item**(而非固定在 Scaffold 内 Column)，随内容滚走。否则头部固定、只有列表滚，大标题不折叠/不连贯。左右分栏(如菜系档)由右侧列表驱动折叠，左侧 rail 独立 scroll 不干扰。列表头部新增 item 会打乱字母跳转偏移——`animateScrollToItem` 基数须 +头部 item 数并纳入 remember key。
- 全屏 `Dialog`/覆盖层会**盖住下沉进列表的搜索行**：覆盖层需自带搜索框(进入即 `FocusRequester` 聚焦)让搜索时仍可改词(iOS Mail 式)，别指望被盖住的列表内搜索行。
- **`AppTopBar` 能力**：`title`(String)+可选 `onBack`(空则不显返回图标，天然支持"同屏两路由：Tab 落地无返回/push 有返回")+可选 `subtitle`(两行标题，各限 1 行省略)+`actions`。标题是搜索框/空标题透明栏/用 Close 图标的全屏模式等**特例保留内联**，别硬塞 AppTopBar 污染其简洁契约。

### 9.16 多操作收进 ActionSheet
- 列表项/卡片的多个操作(编辑/复制/删除/收藏)收进 `ActionSheet`(长按或"⋯"触发)，破坏项 `destructive=true` 红字、"取消"自动置底；禁把多操作排成一行小图标或塞进居中 AlertDialog。

### 9.17 未保存返回守卫用统一封装
- 有 `isDirty` 的编辑表单统一套 `UnsavedGuard`(内部管 `BackHandler`+顶栏返回+"放弃未保存的更改？"弹框，放弃=红字/继续编辑)，禁每屏内联复制。"放弃编辑"属主动丢弃、保留确认；"删除数据"属可逆、走撤销(区别见 9.12)。

### 9.18 一级主分类栏：胶囊分段视觉统一（固定均分 / 可滚不均分同一套 token）
> [AI生成 2026-07-17] 一致性修复：菜品页(`SegmentedControl`,4项固定均分)与食材页(`FilterChip` 横滚,6项可扩)一级主分类**视觉语言不统一**(一个胶囊分段、一个 M3 chip)。用户接受"菜品固定/食材可滑"的**行为差异**，但要求**视觉样式统一**成一套设计语言。**结论：统一到"胶囊分段视觉"(方向③+①融合)——抽共享组件 `PrimaryTabRow`，同一套 token，用 `scrollable` 参数切"均分/横滚"两形态。**

**为什么不选"两页都 FilterChip"**：M3 `FilterChip` 是圆角矩形描边 chip，选中态是"描边+浅底+前导✓"，视觉偏 Material 不够 iOS；菜品 4 项均分铺满时 chip 行会留大片空隙或需手动撑宽，不如胶囊分段规整。**为什么不选"仅统一 token 保留两控件"(纯③)**：两种控件的选中态几何(滑块 vs 描边 chip)本质不同，光调色调不圆角仍是"两个东西"，达不到"看起来同一套"。故取**胶囊分段为唯一视觉**，食材页放弃 FilterChip 改为**可横滚的胶囊分段**(分段轨道整体可滚、项按内容宽度而非均分)。

**统一视觉规范(明暗都成立，全部走主题 token)**：
| 维度 | 取值 | token |
|---|---|---|
| 轨道圆角 | 10dp | `RoundedCornerShape(10)` |
| 选中滑块圆角 | 8dp | `RoundedCornerShape(8)` |
| 轨道底色 | `surfaceVariant` | 现 `SegmentedControl` 同款 |
| 轨道内 padding | 3dp | — |
| 选中滑块底色 | `surface`(白/深卡) | 现 `SegmentedControl` 同款 |
| 选中文字色 | `onSurface` | — |
| 未选文字色 | `onSurfaceVariant` | — |
| 字号/字重 | `labelLarge`；选中 SemiBold / 未选 Normal | — |
| 项竖直 padding | 7dp(触达≈40dp,配轨道 padding 达 ~46dp) | — |
| 项水平 padding | 均分态 4dp；**横滚态 14dp**(项按内容宽,给足呼吸) | — |
| 项间距 | 2dp | — |
| 外层边距 | 水平 16dp、竖直 6dp(两页一致) | — |

**两形态套用同一 token**：
- **菜品(固定4项)** → `PrimaryTabRow(scrollable=false)`：`Row` + 每项 `weight(1f)` **均分铺满**，水平内距 4dp。等价现 `SegmentedControl`(几乎零改，仅换调用名)。
- **食材(可扩≤6项)** → `PrimaryTabRow(scrollable=true)`：轨道换 `Row`+`horizontalScroll`(**禁用 `LazyRow`**——滑块选中态要跨项连续背景、且项数≤10 无虚拟化必要)，项**不 `weight`、按内容宽度**、水平内距加大到 14dp。选中滑块、圆角、配色与菜品**逐像素一致**，只是整条可左右滑、末项被截时提示"还有更多"。

**共享组件签名(抽 `component/PrimaryTabRow.kt`，`SegmentedControl` 保留供 2~5 项互斥场景如推荐模式/去重周期，或让其内部复用本组件的均分分支)**：
```kotlin
@Composable
fun PrimaryTabRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,   // false=均分铺满(菜品) / true=横滚不均分(食材)
)
```
内部：`scrollable=false` 走 `Row{ 项.weight(1f) }`；`true` 走 `Row(Modifier.horizontalScroll(rememberScrollState())){ 项.wrapContentWidth,水平14dp }`。两分支共用同一 `trackShape/thumbShape/背景/文字色/字重`常量，保证视觉唯一真相源。

**落点**：
- 菜品 `DishesScreen.kt` L210 `SegmentedControl(...)` → `PrimaryTabRow(..., scrollable=false)`(签名兼容，`onSelect`/`selectedIndex` 逻辑不动，`setSortTab`/`savedSortTab`/`savedCuisine` 保留)。
- 食材 `IngredientPickerScreen.kt` L262 `LazyRow{ items{ FilterChip } }` → `PrimaryTabRow(options=visibleTabs.map{it.label}, selectedIndex=visibleTabs.indexOf(ui.mainTab), onSelect={ vm.selectMainTab(visibleTabs[it]); selectedIngredient=null }, scrollable=true)`。`visibleTabs` 过滤(库存挂钩)与 `selectMainTab` 逻辑不动，仅换渲染层。
- 删除食材页对 `FilterChip`/`LazyRow`(仅主分类用途)的 import(若二级筛选 `DishFilterChips` 仍用 FilterChip 则保留)。

**风险/兼容**：
- M3 1.1.2 无 `SegmentedButton`——本方案全自绘、不依赖新 API，无兼容问题。
- **横滚放弃均分**是刻意取舍：项数可变(库存 Tab 条件显隐→5或6项)无法均分铺满而不留白，横滚+内容宽是苹果(iOS Mail/App Store 分段横滚)正解。
- 别破坏现有逻辑：`sortTab`(菜品)/`mainTab`(食材)的选中判定、`savedSortTab`/`mainTabInited` 守卫、`selectMainTab(force)`、切 Tab 清 `selectedIngredient`/`savedCuisine` 全部保留，本次**只换视觉外壳不动状态机**。菜品"菜系档带左栏"的 `isCuisineTab` 分支不受影响(仍在分段栏下方)。
- 食材页横滚态**首项左内边距**与搜索行左对齐(外层水平 16dp 起)，选中项若在滚动区外不自动滚入(可选增强：`animateScrollTo` 到选中项,非必须)。
- **真机验证点**：①两页并排看圆角/选中滑块/配色/字重**肉眼同一套**；②食材 6 项可左右滑、滑块选中态连续无割裂；③菜品 4 项仍均分铺满无留白；④明暗两主题下轨道/滑块/文字对比正常；⑤切 Tab 不误清菜系/不重置守卫、库存挂钩关时食材仅 5 项且不崩。

### 9.19 搜索就地新建：结果末尾常驻「新建」行（通讯录/备忘录式）
> [AI生成 2026-07-18] 菜品/食材搜索，"有结果但要找的不在其中"与"0 结果"两态都要能**就地新建**。范式=iOS 通讯录/备忘录搜索：**结果列表末尾常驻一条 `新建xxx「关键词」` 行**（跟随内容、非 sticky 悬浮），0 结果时该行紧接"没找到「X」"说明。菜品与食材统一。抽共享组件 `SearchCreateRow(keyword, entity, onClick)`（48dp 行·`Icons.Outlined.Add` 20dp 主色·`bodyLarge` 主色文案`新建${entity}「${keyword}」`·上方 `Divider(outlineVariant)`·`keyword.isBlank()` 守卫）。

- **文案**：一律「新建」不用「添加」——本项目"添加"=把已有项加入某餐/某菜，"新建"=从无到有造库记录，二者区分；0 结果说明用「没找到」不用「未找到」（更口语·不责备）。
- **点击**：一律 `keyword.trim()` **回填**到新建表单名称字段（复用现有 `NewDishPrefillBus`/`IngredientCreateBus`/`createPrefillName+loadIngredientEditor`）——少一步输入；食材编辑器 `initialName` 会顺带 `guessNutrition` 预填营养大类。
- **显隐由回调决定**（红线）：`SearchResultsPanel` 用可选参 `onCreateNew: (()->Unit)? = null` + `createKeyword`，传入才渲染末尾行（别用 `mode` 布尔硬编码）。
- **边界**：关键词空/纯空格不显新建行；末尾行跟随列表非 sticky（不遮结果）；超长 ellipsis、回填用完整 trim 词；选择器（DishPicker）沿用其**底部常驻行**范式（有词时文案切 `新建菜品「X」`），其余搜索面用"列表末尾行"；全局混合搜索页 `SearchScreen` 暂不做（三类混合不适合每类末尾塞新建行）。
- **落地**（2026-07-18 已实现·过双门禁）：`SearchCreateRow.kt`(新增) + `DishesScreen.DishSearchOverlay` + `DishPickerScreen`(底部行) + `IngredientPickerScreen`(SearchResultsPanel 末尾行 + selectionMode 分叉收敛为"关键词非空即显面板")。

### 9.20 负反馈「踩」：长按 ActionSheet + 就地灰态 + 撤销
> [AI生成 2026-07-18] AI 推荐结果里让用户**显式**标"不再推荐这道菜"(只用户主动标才降权，不做"未选=不喜欢"隐式判断防误伤)，标了的菜后续推荐里沉底/滤除。范式=**长按菜卡 → ActionSheet(destructive「不再推荐这道菜」) → 就地淡出灰态(不移除不跳动·隐勾选圈) → Snackbar「撤销」(§9.12 防误标) → 持久化负信号由算法过滤，恢复走长按「恢复推荐」或菜品详情**。全程零新建视觉组件、不诱导、可逆。

- **入口=长按**(非常驻👎图标·非右滑)：推荐菜卡整行已被"点击=勾选加入这一餐"占用，长按走上下文菜单符合苹果惯例、不占视觉不诱导否定；`DishRow` 的 `.clickable{onToggle}` 改 `.combinedClickable(onClick=onToggle, onLongClick=onDislikeRequest)`，勾选圈自身 `.clickable` 保留(点圈仍只勾选)。低频负反馈藏长按、高频勾选留右侧。
- **反馈=就地灰态不移除**：标记瞬间该行 `alpha 0.5` + 名后浅字"已标记不再推荐" + 隐勾选圈(同步从 selectedIds 移除已勾的)；不立即删行(避免列表跳动、"东西没了")。同时 `AppSnackbar.showUndo("已不再推荐「X」", 撤销)`(时长 Long)。下次推荐/换一换时该菜由算法(gather 过滤 disliked)自然不再出现。
- **恢复**：撤销窗口内 Snackbar「撤销」；过后该菜仍在当前灰态列表→长按「恢复推荐」；已滚出/被过滤→**菜品详情页**"已设为不再推荐 + 恢复推荐"闭环。独立"不再推荐列表"管理页=二期(过度设计,家庭场景踩得少)。
- **文案**：统一「不再推荐」(不用"不喜欢"=情绪化/评判菜品、不用"不常吃"=频率不精确)；恢复=「恢复推荐」；状态"已标记不再推荐"。规则说明弹层可追加"你标了『不再推荐』的菜会明显靠后甚至不再出现(可在菜品详情恢复)"守诚实。
- **边界**：忌口菜(已标红排末)仍可再踩(忌口=系统建议避免·踩=个人不想看·语义不同可叠加)；随机/库存/模型搭配卡均带长按踩(复用同一 DishRow)；周期计划编辑态**不加**踩入口(其长按语义应是"换这天的菜")，但踩的负信号**作用于计划生成**(gather/gatherForPlan 过滤)；踩正被勾选的菜→同步移除已勾、撤销不自动回勾。
- **落地**：`dish` 加 `disliked` 列(`27.sqm` 迁移) + `setDishDisliked`/`selectDislikedDishIds` 查询 + `RecommendationDataSource.gather`/`gatherForPlan` 过滤 disliked + VM `setDisliked` + `DishRow` 长按 ActionSheet + 菜品详情恢复入口。

### 9.21 餐次分类：固定多选 toggle chip · 二级横滚栏 · 搜索按餐次 · 记一餐预筛
> [AI生成 2026-07-19] 菜品加"可存储多值餐次属性"(dish_meal_slot)。四处交互范式(过 Apple-UX 门禁)：

- **编辑页"适合餐次"= 固定多选 toggle chip**(`MealSlotChip`)：6 餐次固定项，**实心(primary)=选中 / 描边灰(surfaceVariant)=未选，无 ×、无 ✓**——刻意区别于"增删型"AssistChip(菜系/烹饪方式带 ×/+添加)，让"这排是勾选"和"那排是增删"一眼可分。位置=折叠区**上方**、可见非必填(让用户一眼确认智能预选对不对)。chip 高 ~32dp、圆角 16dp(全胶囊)、水平内距 14dp、间距 8dp、选中 SemiBold。
- **智能预选 + 手动锁定(减法反馈)**：新建按菜名 `MealSlotMatcher.defaultSlotsFor` 预选并显一行浅灰"已按菜名智能预选，可增减"；**用户手动碰任一 chip → `mealSlotTouched=true` → 提示行消失**(用"提示消失"这一个减法反馈传达"现在你说了算"，不加锁图标=苹果式克制)。编辑既有菜回显存储值;老库未打标则同样 Matcher 预选(编辑即预填、保存即应用)。空选保存时 Matcher 兜底(永不无餐次菜)。
- **菜品页二级餐次栏 = §9.18 横滚胶囊分段·常驻不隐**：一级 Tab 正下方加二级 `PrimaryTabRow(scrollable=true)`(全部+6餐次)，作用**所有档**，"全部"=高亮首项即不筛。**推翻方案原文"全部时隐栏"→改常驻**(核心横切维度藏起来降发现性;栏仅 ~46dp)。放 Column **固定层不进 LazyColumn**(与一级 Tab 行为一致吸顶不随列表滚、且不打乱字母跳转 `letterHeaderCount` 偏移)。
- **搜索按餐次**：搜索框整词命中纯餐次词(早餐/午餐/晚餐/加餐/宵夜…)→切"按餐次筛"模式查 `mealSlots`，头部换**淡主色条 + 餐具图标 + "适合早餐的菜品 · N 道"**(区别于普通"搜索结果 N")；**餐次模式不显末尾"新建菜品「早餐」"行**(§9.19 显隐由回调决定，避免"新建叫早餐的菜")；0 结果文案用餐次口径"还没有适合早餐的菜品"。
- **记一餐按餐次预筛(告知不替决定)**：选菜弹窗按当前餐块餐次默认"只看适合早餐(N)"一枚可切 chip，**点一下切"显示全部"**(不硬隐藏、把决定权留用户)。加餐(SNACK)/未知餐次→不预筛。搜索时不预筛(搜索全局)。
- **落地**：`28.sqm`+`dish_meal_slot` 表 · `MealSlotMatcher.defaultSlotsFor`(降默认推断器·恒非空) · `Dish/DishMini.mealSlots` · `DishRepository`(批量载+兜底/存储/全量替换) · `seedDishMealSlots`(补齐式打标+`SEED_LOGIC_VERSION`v6) · `RecommendationDataSource` 改查 `DishMini.mealSlots` · `NewDishScreen`/`DishesScreen`/`DishPickerScreen`。

### 9.22 个人忌口设置：按分类固定多选 toggle chip（复用共享 ToggleChip）
> [AI生成 2026-07-19] 家庭成员各自的"口味忌口"(不吃羊肉/奶/葱蒜)，与健康调养忌口正交取并集。放**成员编辑弹层**「不吃的食材」分区(健康状态之后)。

- **chip = 共享 `ui/component/ToggleChip`**(§9.21 提取的实心/描边 toggle 胶囊)——忌口分类 chip 与餐次 chip 同类多选，**单一源共享**(MealSlotChip 已提为 ToggleChip·防内联复制漂移)。
- **15 个分类 chip 两组分层**：荤食(猪肉/牛肉/羊肉/鸡鸭禽肉/动物内脏/鱼/虾蟹/贝类/蛋) + 素食与口味(奶类/大豆坚果/菌菇/藻类海带/葱蒜/香辛料)。`FlowRow` spacedBy 8dp,组间小标题 `labelMedium+primary`。
- **信息层级**：荤类**只给具体子类**(羊肉/鱼…)不给"肉类"父级 chip(避免大类与子类并列困惑·"不吃所有肉"几乎不存在);仅"可能整类不吃"的类(奶/豆/菌/葱蒜)给大类 chip。
- **无智能预选**(纯人为声明·默认空·不猜测防误加)、**无锁定提示行**、点亮即选再点取消、无二次确认。
- **说明句**"选了就不给这个人推荐，调味也一起避开"兼作引导 + 调料真避开提示(个人忌口对调料真避开·含即命中·区别于健康忌口"少放")。无独立空态(chip 全灰即引导)。
- **数据**：`food_category` **无 code 列**→按**分类名**白名单映射 id(缺失静默跳过);chip 用口语短标签(非分类库 name);`FamilyRepository.listAvoidCategoryOptions()` 一次取。
- **落地**：`29.sqm`+`member_avoid_category` 表 · `FamilyMember.avoidCategoryIds` · `HealthConstraints.personalAvoidIngredientIds`(单独字段·含所有角色) · `HealthRuleEngine` 合并进 avoidNames · `RecommendationDataSource` gather/gatherForPlan 全家并集分类→食材ids · `FamilyViewModel`/`FamilyScreen.MemberEditorDialog`。

### 9.23 同时关注多人：三处切换器 + 星标多选（多选关注·当前查看指针·非聚合）
> [AI生成 2026-07-19] 单关注→多关注。**零 schema 迁移**(`is_focus` 改可多选·当前查看指针存偏好JSON)。核心=**多选关注 + 一个"当前查看"指针**(不做聚合合并·热量/份额是个人概念不可加总)。**最高优先级:1人时逐像素零变化**。

- **显隐门禁(三处切换器共用)**：`focusMembers = members.filter{isFocus}`(约束 size≥1)；**size==2 才显切换器**·size==1(含全部老用户)整块不渲染(无空行/无控件/无逻辑分叉)=零变化。
- **今日卡成员切换 chip 行**：≥2 关注人时卡内顶部(标题上方)加 §9.18 `PrimaryTabRow(scrollable=true)`——**单选指针**(白滑块=当前查看人·非 ToggleChip 多选)·点即切指针整卡换该人视角(热量/宏量/达标/concerns)·**不轮播不聚合**·间距 chip↔标题 10dp。当前查看人今日缺席仍显 chip·卡走空态。
- **报告个人视角切换器**：`personal && focusMembers.size≥2` 时"家庭/个人"控制区下方加成员切换——**2~4人 `SegmentedControl`(均分) / >4人 `PrimaryTabRow(scrollable)`**(token 同源·临界无割裂)·与今日卡**共用同一指针**(一处切两处同步)。切"家庭"视角整行消失。
- **家庭页星标单选→多选**：`Icons.Filled.Star`(实心 primary)=已关注 / `Icons.Outlined.StarBorder`(描边)=未关注·**可多个同时实心**(去 clearAll·`toggleFocus`)·即点即切无二次确认。**取消最后一个**→repo 拒绝+`AppSnackbar`"至少关注一位家人"(星保持实心)。说明句"⭐关注的家人会出现在今日营养卡和报告里，可关注多位、看时一键切换"。
- **职责划分**：家庭页**只管"关注谁"**·"当前看谁"交今日卡/报告切换器(天天用)——两处都能切会困惑·苹果"一个动作一个地方"。
- **迁移零风险**：老库唯一 `is_focus=1` 那人=关注集合 size=1·指针空→`resolveViewing` 回退关注集合首位=旧 `pickFocus` 行为·100%无感。
- **落地**：`FamilyRepository.resolveViewing`(收口)/`toggleFocus`/`setViewingMember`/`observeViewingMember`·`PreferenceRepository.observe/setFocusViewingMemberId`(偏好指针)·`updateMemberFocus` 查询·`FamilyScreen`星标多选·`HomeViewModel.focusSwitcher`+`NutritionTodayCard` chip 行·`DietReportViewModel`+`DietReportScreen` 成员切换器。零 `.sqm`。
