# 「分享链接解析·外部菜谱导入」L4 Phase1 交互视觉规范（Apple-UX 定稿）

> 产出：apple_ux_designer 门禁审 · 2026-09-03 · 基于《分享链接解析_方案设计 v1.1》+《苹果风格UI设计方案》§九（9.1–9.44）+《交互组件复用指南》22 组件 + NewDishScreen/DishesScreen/HomeScreen 等既有范式实码
> 所有色值走 `MaterialTheme.colorScheme` 语义 token（自动随 AppPalette 明暗），语义状态色走 `ExtendedColors`，禁新增硬编码色。
>
> **签名元素（全功能唯一记忆点）**：ParseSheet 结果态顶部的「菜谱卡」——16:9 封面 + 菜名 + 来源行的组合。其余一切（隐私弹窗、列表页、横幅、红点）全部用标准控件与既有范式搭建，零装饰。
>
> **反 AI 感自检结论**：结构轴（三态 Sheet、AlertDialog 二选一、勾选逻辑、折叠阈值、触达尺寸）严格随平台惯例与本项目既有范式；取值轴为本产品语境定制的仅三处——来源胶囊文案「下厨房」、「新」徽标语义（你的食材库里还没有）、横幅文案指向「菜品页右上角入口」。

---

## A. ParseSheet 解析 BottomSheet（`ui/link/ParseSheet.kt`）

### A.0 选型确认

- 载体 = **M3 `ModalBottomSheet`（1.1.2 实验可用，项目已 7 处在用）**，`rememberModalBottomSheetState(skipPartiallyExpanded = true)`——三态是同一 Sheet 的内容态切换，**不是三个弹层**，避免层叠闪烁。
- **dragHandle 保留默认**（顶部 grabber 小横条）；**scrim 保留默认**；`onDismissRequest` = 关闭 Sheet，链接已落库（parse_state 不变），可从链接列表找回——关闭即安全，无守卫。
- 「加载网页」与「提取菜谱」两个阶段**合并为一个视觉态**：正则解析是毫秒级，文案直接从加载跳到成功/失败；仅留两段文案用于极端慢机型的文案过渡（spinner 连续不停顿）。
- 组件契约（能力显隐由回调决定，§9.3 红线）：

```kotlin
@Composable
fun ParseSheet(
    state: ParseSheetUiState,          // Loading(source, url, phase) | Success(recipe) | Failed(source)
    onSaveDish: (() -> Unit)?,         // null 则不渲染主 CTA
    onRetry: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    onSaveLinkOnly: (() -> Unit)?,
)
```

### A.1 态一：加载中（视觉约 0.4 屏，wrapContent 不强制高度）

```
┌──────────────────────────────────┐
│           —— grabber ——          │
│                                  │
│        ◌ (spinner 24dp)          │
│      正在加载网页…                 │  ← bodyLarge / onSurface
│      来自下厨房                    │  ← bodySmall / onSurfaceVariant
│   m.xiachufang.com/recipe/…      │  ← labelSmall / outline，居中，maxLines=1 ellipsis
│                                  │
│         [ 仅保存链接 ]            │  ← TextButton
│                                  │
```

| 元素 | 规格 |
|---|---|
| 容器 | `Column` 居中，`padding(horizontal 20dp, vertical 28dp)`，**wrapContentHeight**（约 0.4 屏） |
| spinner | `CircularProgressIndicator(24dp, color = primary)`；慢机型文案切换 `正在加载网页…` → `正在提取菜谱…` |
| URL | `labelSmall` + `colorScheme.outline`，居中，只显示 host+path（工程层去 query 参数，防 utm 长串） |
| 退出 | `TextButton("仅保存链接")`（回调传入才渲染），上方 `Spacer(16dp)` |

### A.2 态二：解析成功（固定 `fillMaxHeight(0.85f)`，内部分滚动区 + 固定 CTA）

```
┌──────────────────────────────────┐
│ —— grabber ——                    │
│ ┌──────────────────────────────┐ │
│ │      封面 16:9 圆角12          │ │  ← 已下载本地图(Crop) / 灰底占位
│ └──────────────────────────────┘ │
│ 巨鲜美的冬瓜丸子汤                 │  ← titleMedium SemiBold onSurface，maxLines 2
│ 来自下厨房                 (煮)   │  ← labelMedium 灰  +  做法胶囊(圆角4)
│                                  │
│ 食材 · 7 种（新 2 种）            │  ← 分区头
│ 冬瓜 500克  猪肉馅 300克(新)      │  ← FlowRow，纯文本 + 新徽标
│ 姜末 4片  鸡蛋 1个  盐 适量 …     │
│         [ 展开全部 9 种 ]         │  ← 仅 >8 种时
│                                  │
│ 步骤 · 4 步                       │
│ ① 猪肉馅➕姜末…搅拌上劲           │  ← 序号圆18dp + bodyMedium
│ ② 冬瓜去皮切块…大火煮10分钟       │     折叠态每步 maxLines=2 ellipsis
│ ③ 转最小火，虎口挤出肉丸…         │
│         [ 展开全部 4 步 ]         │  ← 仅 >3 步时
│ ├────────────────────────────┤   │
│ │      [ 存为菜品 ]  (胶囊通宽)   │ │  ← 固定底部，Divider 之下
│ │       在浏览器中查看            │ │
└─┴──────────────────────────────┴─┘
```

| 元素 | 精确规格 |
|---|---|
| 容器 | `Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f))`；**上部** `Column(weight(1f).verticalScroll())` `padding(horizontal 20dp)`；**下部** 固定 CTA 区 |
| 封面 | `Box(fillMaxWidth().aspectRatio(16/9).clip(RoundedCornerShape(12dp)))`；图 `ContentScale.Crop`（§9.32）；**无图占位** = `surfaceVariant.copy(alpha 0.4f)` 底 + 中心 `Icons.Outlined.Image(24dp, onSurfaceVariant)`。上 12dp 下 12dp 间距 |
| 菜名 | `titleMedium` + `FontWeight.SemiBold` + `onSurface`，`maxLines = 2` ellipsis |
| 来源行 | `Row(CenterVertically)`：`Text("来自下厨房", labelMedium, onSurfaceVariant)` + `weight(1f)` + 做法胶囊；上 6dp |
| 做法胶囊 | 与编辑页「待自建」**同 token**（零新样式）：`Surface(RoundedCornerShape(4dp), surfaceVariant)` 内 `Text(method, labelSmall, onSurfaceVariant, padding(h6,v1))`；空则整枚不渲染；`contentDescription = "做法：煮"` |
| 分区头 | `Text(titleSmall, SemiBold, onSurface)`，上 16dp 下 8dp。文案：`食材 · 7 种（新 2 种）` / `步骤 · 4 步`；无新食材时省略括号段 |
| 食材项 | `FlowRow(spacedBy(6dp, 6dp))`，每项 `Row(CenterVertically)`：`Text("冬瓜 500克", bodyMedium, onSurface)`（**显示解析原样，忠于网页**；克→g 折算发生在预填层）+ 新食材附「新」徽标 |
| 「新」徽标 | §9.4 贴角小圆：`Box(14dp, CircleShape, primaryContainer)` 内 `Text("新", 9sp, onPrimaryContainer)`；`contentDescription = "食材库还没有，保存时自动新建"` |
| 食材折叠 | ≤8 种全显；>8 种显前 8 + `TextButton("展开全部 N 种")`，居中，上 4dp |
| 步骤行 | `Row(Top)`：序号圆 `Box(18dp, CircleShape, surfaceVariant)` 内 `Text("1", labelSmall, onSurfaceVariant)` + `Spacer(8dp)` + `Text(正文, bodyMedium, onSurface)`；折叠态 `maxLines = 2` ellipsis；行间 `Spacer(8dp)` |
| 步骤折叠 | ≤3 步全显；>3 步显前 3 + `TextButton("展开全部 N 步")`，居中 |
| 固定 CTA 区 | `Divider(1dp, outlineVariant)` → `Column(Modifier.navigationBarsPadding().padding(horizontal 16dp, vertical 12dp))`（与 ActionSheet 底距写法逐字一致）：`CapsuleButton("存为菜品", Modifier.fillMaxWidth().heightIn(min = 48dp))` + `Spacer(6dp)` + `Row` 居中单枚 `TextButton("在浏览器中查看")` |
| 三态转场 | 无自定义转场。内容高度变化时 Sheet 自然 animate；spinner→内容直接替换（克制，不叠 crossfade） |

### A.3 态三：解析失败（wrapContent，约 0.4 屏）

```
        (LinkOff 28dp 灰)
       没法解析这个链接
  这个网页不是菜谱页，或网络不太好
       [ 重试 ](胶囊，居中wrap)
       [ 仅保存链接 ](文字)
```

| 元素 | 规格 |
|---|---|
| 容器 | `Column` 水平居中，`padding(horizontal 20dp, vertical 28dp)` |
| 图标 | `Icons.Outlined.LinkOff(28dp, onSurfaceVariant)`（链接语义，不用 Error 红大图标——失败不吓唬） |
| 标题 | `titleMedium SemiBold onSurface`：「没法解析这个链接」 |
| 副文案 | `bodyMedium onSurfaceVariant`：「这个网页不是菜谱页，或网络不太好」（不责备） |
| 按钮 | 上 20dp：`CapsuleButton("重试", heightIn(min=48dp))` 居中不通宽；+ 8dp `TextButton("仅保存链接")` |

---

## B. 存为菜品 → 图片下载 → 跳转编辑页

### B.1 下载策略（修正方案 v1.1）：预下载，而非点击后才下载

**解析成功展示结果态的同时即开始后台下载封面图**（同一份文件直接复用为预填 `imagePath`）：

- 结果态封面绝大多数在用户阅读菜谱的 1–2 秒内已就绪 → 点「存为菜品」时**几乎零等待**；
- 点击时刻未完成 → 按钮进入 loading 态等它，**等满剩余时间（上限 3s）**；
- 失败/超时 → 静默跳转，无图进编辑页。

零新增图片加载依赖（项目无 Coil/AsyncImage，本方案也**不建议为此加**——本地优先气质 + 下载本来就必然发生，`StoredImage` 体系直接复用）。

### B.2 点击「存为菜品」的 3 秒感知设计

- **按钮就地转 loading**（不弹层、不换页等待）：胶囊同 `CapsuleButton` 几何（fillMaxWidth、heightIn(min 48dp)），内容切换为 `Row(CenterVertically)`：`CircularProgressIndicator(16dp, onPrimary)` + `Spacer(8dp)` + `Text("正在导入…", labelLarge, SemiBold)`；**同时禁点**（防双击双跳）。
- 文案只用「正在导入…」一个词——如实覆盖「等图 + 跳转」两件事。
- 图就绪/超时 → dismiss Sheet + 跳转 `NewDishScreen`。跳转动效：Sheet 下滑退场 + NavHost 默认页面过渡，**不加自定义动效**。
- 理由：最长 3 秒的不可控网络等待，确定性进度是谎言；spinner + 禁点是苹果对短等待的标准答案。

### B.3 下载失败（无图进编辑页）——不提示

- 封面本来就是选填：编辑页顶部封面位是**虚线引导卡**（`ImagePickerButton` coverStyle 空态），「没图、可点添加」在界面上自可见（T1 可查即可）；
- 编辑页已有导入 Snackbar（C.2）占据反馈位，再叠一条 = 弹窗疲劳；
- 用户可后补图（编辑页点封面即可），路径不丢。

---

## C. 编辑页预填后的呈现（NewDishScreen 既有页面，零新组件）

### C.1 首屏焦点顺序（导入跳入后自上而下）

| 屏位 | 内容 | 状态 | 既有机制覆盖 |
|---|---|---|---|
| 顶部 | 封面 16:9 通栏 | 有图显图（Crop）/ 无图虚框引导卡 | `ImagePickerButton(coverStyle)` 既有，预填 `imagePath` 即显 |
| 1 | 菜名「巨鲜美的冬瓜丸子汤」 | 已填 | `applyPrefill` 既有 |
| 2 | 食材清单 | N 项、主料/非主料分组、库外项带灰底「待自建」小胶囊 | `IngredientNameCell(pendingCreate)` 既有（NewDishScreen:1260） |
| 3 | 适合餐次 | 按菜名预选 + 既有提示行「已按菜名智能预选，可增减」 | v28 既有，**文案不变**（餐次是本地按菜名推断，原文案准确） |
| 4 | 操作步骤（折叠段） | **自动展开**、4 步草稿 | `LaunchedEffect(state.steps.isNotEmpty()){expandSteps=true}` 既有（:372） |
| 5 | 更多信息（折叠段） | **自动展开**、做法 chip「煮」+ 新增导入提示行 | `moreHasContent` 含 `cookingMethodNames` 非空天然触发（:373-375），零改动 |

折叠区自动展开后，首屏仍是封面 + 菜名 + 食材清单——用户第一眼看到的正是「导入带来了什么」。

### C.2 库外食材「保存时将创建」的告知——复用 autoAddMessage Snackbar 通道

- 既有机制：「待自建」灰底胶囊**持续在场**（零改动保留）+ `autoAddMessage` Snackbar（一次性，`LaunchedEffect(autoAddSerial)` 已接全局宿主）。
- 落地：导入预填完成后，`applyPrefill` 内置 `autoAddMessage` + `autoAddSerial+1`，**复用既有 Snackbar 通道与句式**，文案加导入分支：
  - 无新食材：「已导入 7 项食材」
  - 有新食材：「已导入 7 项，其中「猪肉馅」等 2 味将在保存时加入食材库」（「将在保存时加入食材库」**逐字复用**既有文案）
- **不选内联提示行的理由**：编辑页已有餐次、做法两条灰字提示行，再叠第三行 = 三行灰字堆叠；计数型动态信息用一次性 Snackbar，「来源解释型」信息才用常驻提示行。

### C.3 导入烹饪方式与「已按菜名预选，可改」提示行的关系——区分，不统一

- 事实核查：导入若走 `addCookingMethod` 置 `cookingMethodTouched`，既有提示行条件 `cookingMethodPrefilled && !cookingMethodTouched`（:401）为 false → 提示行消失。
- 必须区分的理由：对「冬瓜丸子汤」这类菜名，说「已按菜名预选」是**失实**（做法来自步骤文本推断/网页数据）——诚实准则不允许；网页给的做法与本地推断可信度不同，用户有权知道来源。
- 落地：UiState 加 `cookingMethodImported: Boolean`；`addCookingMethod` 加参数 `markUserTouched: Boolean = true`（导入路径传 false，同时置 `imported = true`；菜名推演跳过条件改 `touched || imported`——导入值不被推演覆盖）。提示行三态：

| 态 | 条件 | 文案 |
|---|---|---|
| 本地菜名预选 | `prefilled && !touched` | 已按菜名预选，可改（既有，不动） |
| 链接导入 | `imported && !touched` | **来自菜谱导入，可改** |
| 用户手动碰过 | `touched` | 不显示（既有减法反馈，不动） |

- 样式与既有提示行逐字同 token：`bodySmall` + `onSurfaceVariant` + `padding(bottom 6dp)`，位置在「烹饪方式」FormFieldLabel 之下。

---

## D. 链接列表页（`ui/link/LinkListScreen.kt`）

### D.1 骨架

- `AppTopBar(title = "导入的菜谱", onBack = …)`（§9.15 带返回二级页判据，无 actions——删除收长按、不设顶栏垃圾桶）。
- `LazyColumn`；底部避让由 MainScaffold 无底栏路由统一 `navigationBarsPadding()`，页内仅 `contentPadding(bottom 16dp)`。

### D.2 行布局（整行可点，触达 ≥56dp）

```
┌────────────────────────────────────────┐
│ (下厨房)  巨鲜美的冬瓜丸子汤              │  ← 胶囊 + titleSmall SemiBold，maxLines 2
│ m.xiachufang.com/recipe/106…  ● 待解析  │  ← URL 灰 + 状态点文字（第二行右侧）
├────────────────────────────────────────┤
```

| 元素 | 规格 |
|---|---|
| 行容器 | `Column(padding(horizontal 16dp, vertical 10dp))` + `Modifier.clickable(onClick)`，`heightIn(min = 56dp)` |
| 来源胶囊 | 与「待自建」/做法胶囊**同一 token**：`Surface(RoundedCornerShape(4dp), surfaceVariant)` + `Text(labelSmall, onSurfaceVariant, padding(h6,v1))`；它是行的视觉锚 |
| 标题 | `titleSmall` SemiBold `onSurface`，`maxLines = 2` |
| URL | `labelSmall` + `outline` 色，`weight(1f)`，`maxLines = 1` ellipsis，host+path |
| 状态标签 | 第二行行尾：**6dp 语义色圆点 + `labelMedium` 文字**双编码：待解析 = 琥珀（`ExtendedColors.warning`）+「待解析」；解析失败 = 红（`error`）+「解析失败」；已存为菜品 = 绿（`ExtendedColors.success`）+「已存为菜品」 |
| 行间距 | 组内 `Spacer(2dp)`；分组已由 SectionHeader 承担，组内不加分隔线 |

### D.3 分组排序的视觉表达

- 三组各一枚 SectionHeader：`"待解析 · 2"` / `"解析失败"` / `"已存为菜品"`（`· N` 内联计数；0 项的组整组不渲染含 header）。
- 组序 = 待解析 → 解析失败 → 已存为菜品，组内 `created_at` 倒序——**不用分割卡/底色区分组**。
- 点击行为按状态分派：待解析 → 先过 F 节 T2 弹窗再进 ParseSheet；解析失败 → 直接进 ParseSheet 并**自动开始重试**（少一步）；已存为菜品 → 跳该 `dish_id` 菜品详情（用户心智是「看这道菜」）。

### D.4 删除交互——MVP 用长按 ActionSheet

- 长按行 → `ActionSheet(title = 链接标题, actions = [SheetAction("删除这条链接", destructive = true)])`；已存组加 `message = "只删除这条链接，已保存的菜品不受影响"`（说清影响面）。
- 删除走**软删 + 撤销**（§9.12）：`LocalAppSnackbar.showUndo("已删除链接")`。
- 多选批量删除、左滑删除 → Phase 2（与方案 §九分期一致）。

### D.5 空态

```kotlin
EmptyState(
    text = "还没有导入的菜谱\n在下厨房看到喜欢的菜，分享到「今天吃啥」就能导入",
    icon = "🔗",
)
```

不给 actionLabel——下一步动作在系统分享层（App 内无可点目标），文案把路径说清即「给了下一步」（§9.6 精神）。

---

## E. 入口与提醒

### E.1 DishesScreen 顶栏链接图标

- 落点：actions 序 = 搜索 → **链接** → 加号。
- 图标：`Icons.Outlined.Link`（与全 App `Icons.Outlined.*` 线性语言一致），`contentDescription = "导入的菜谱"`。
- 红点：**M3 `BadgedBox(badge = { Badge() })`**——空内容 `Badge()` 即标准小圆点，默认 `error` 底（iOS 通知 badge 共识色：「有待办」语义，与 §9.27 Tune 的 primary 筛选点区分）。有未解析链接（parse_state=0 计数>0）时显示。

### E.2 HomeScreen 未解析横幅（`LinkPendingBanner`，复用 §9.31 范式）

落点：`NextMealCard` 之后、营养色系墙之前，独立 `item`。结构与 `NutritionHintBanner`（AiRecommendScreen:83）逐参数同源：

| 维度 | 取值 |
|---|---|
| 载体 | `OutlinedCard(shape = shapes.large, containerColor = surfaceVariant.copy(alpha 0.4f))`，`Modifier.fillMaxWidth().padding(horizontal 16dp, top 8dp)` |
| 内距 | `Column(padding(14dp))` |
| 头行 | `Row(Top)`：`Text("🔗", bodyMedium)` + `Spacer(8dp)` + `Column(weight(1f))` |
| 主文案 | `bodyMedium` Medium `onSurface`：「有 2 条分享链接还没解析」 |
| 副文案 | `bodySmall` `onSurfaceVariant`：「来自下厨房的菜谱，点开就能变成你的菜品」 |
| 按钮行 | `Row(End)`：`TextButton("去看看")`（primary）+ `Spacer(4dp)` + `TextButton("不再提醒")`（字色 `onSurfaceVariant`，降级为次操作） |
| **无右上 ×** | 三动作会产生「× 是当天关还是永久关」的歧义，砍掉 ×；「当天不重复」由 VM 自动去重 |

- 「去看看」→ LinkListScreen；「不再提醒」→ 锁一次性偏好 flag + 横幅当帧消失；**不弹确认**——横幅关闭后红点仍在、列表页仍可达，入口不丢。
- **UI 无关性确认**：组件纯 props（`count: Int, onOpen, onMute`）；「当天不展示过」「永久关闭」两个 flag 全在 `HomeViewModel`，组件不感知。

---

## F. 隐私弹窗（T2 事前告知）

- **形态：居中 `AlertDialog`**，不用 BottomSheet。理由：这是「二选一决策」不是「浏览内容」；全 App 居中 AlertDialog 已是 T2/T3 硬同意的既定形态。**无图标**（iOS alert 惯例）。
- 结构（M3 `AlertDialog` 默认布局，不覆写几何）：

| 槽 | 内容 |
|---|---|
| title | 「打开下厨房的网页？」（问句给选择权；来源名动态替换） |
| text | `Column` 两行 `bodyMedium` `onSurfaceVariant`，行距 6dp：①「打开时，下厨房的服务器会看到这次访问。」②「打开后会自动提取菜名、食材和步骤，由你确认后才保存。」（四要素：做什么/谁会看到/提取什么/谁决定） |
| confirmButton | `TextButton("同意并打开")`（动词+对象，比「同意并继续」具体） |
| dismissButton | `TextButton("仅保存链接")` |
| onDismissRequest | 等价「仅保存链接」（点外部 = 用户收回同意，链接保留） |

- 「仅保存链接」后续：**Toast「已保存链接，之后可在 菜品 页右上角打开」→ finish() 回来源 App**。用 Toast 不用 Snackbar：ShareReceiverActivity 是透明宿主、无全局 Snackbar 宿主，Toast 是纯告知（无跟进项）的合规场景（§9.12）。

---

## G. 步骤图展示（v1.1.3 用户拍板纳入 Phase1·主线程按 A 节 token 范式补章）

### G.1 ParseSheet 结果态——折叠不显图、展开显图（渐进披露）

- **折叠态（默认·每步 maxLines=2）不显示步骤图**：纯文本+序号圆紧凑扫读——5 张通栏图叠进折叠态会淹没文本主体；「展开全部 N 步」即用户"我要看细节"的显式信号，图随细节出现。
- **展开后**：每步文本下方 `Spacer(8dp)` + 图 `Modifier.fillMaxWidth().heightIn(max = 96dp).clip(RoundedCornerShape(6dp))`，`ContentScale.Crop`，源=预下载本地图；该步图未就绪/下载失败 = **整块不渲染**（不留灰洞）。
- 无障碍：`contentDescription = "步骤 ${index + 1} 配图"`。

### G.2 编辑页——零新增

预填的 `DishStep.imagePath/thumbnailPath`（**必须经 `encodeImagePaths` 同一编码**）由每步既有 `ImagePickerButton(maxCount=3)` 直接承接（展示/删除撤销/补图·NewDishScreen:778-785）；违规裸塞路径=图不显示（方案红线 12）。

### G.3 菜品详情页——零新增

`dish_step.image_path` 既有展示链路承接，无本批改动。

---

## 附一：全状态清单（每界面）

| 界面 | 状态 | 处理 |
|---|---|---|
| ParseSheet | 加载中 / 成功 / 失败 / 成功但无封面 | 前三态见 A；无封面 = 灰底占位图（不报错） |
| LinkListScreen | 空 / 正常（0–3 组任意组合） / 加载 | 空 = EmptyState（D.5）；本地 DB 直读无慢加载态，VM 未就绪时渲染空列表（§9.43 反骨架屏立场） |
| HomeScreen 横幅 | 有未解析 / 无 / 当天已展示 / 永久关闭 | 组件只渲染「有」，其余三态 = 不渲染 |
| 编辑页 | 有图预填 / 无图预填 / 新食材 0 或 N | 封面虚框卡、待自建胶囊、导入 Snackbar 分别承接 |
| 网络 | 无网分享进来 | WebView 加载失败 → ParseSheet 失败态（副文案已覆盖「网络不太好」），链接已存列表 |

## 附二：无障碍与触达核对

- 主 CTA（存为菜品/重试）`heightIn(min 48dp)` 通宽；顶栏 IconButton 自带 48dp 触达；列表整行可点 ≥56dp。
- 链接图标、红点、做法胶囊、「新」徽标均有 `contentDescription`；状态 = 色点 + 文字双编码（不靠颜色单独表意）。
- 全部色值走 `colorScheme`/`ExtendedColors` 语义 token，明暗两套自动成立。

## 附三：落地批次沉淀要求

本规范落地后：§九 新增 9.45「外部菜谱导入三态 Sheet + 来源胶囊」小节、交互组件复用指南追加 ParseSheet/LinkPendingBanner 两行——由落地批次执行，不在本批。
