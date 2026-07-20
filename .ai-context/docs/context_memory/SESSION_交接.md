# 🔖 SESSION 交接入口（新会话先读这里）

> 会话交接唯一固定入口（每次交接覆盖，历史流水在 git）。
> 触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落地文档+覆盖本文件+git 提交。
> 更新时间：**2026-07-20 交接（无人值守·超长会话·全 push origin/master 到 `aaec3a2`）**。androidApp BUILD SUCCESSFUL + shared 单测全绿；工作区仅 temp/claude。**用户授权持续无人值守推进·每完成一项 commit+push+飞书**。用户设备暂断/外出→**远程 git 验证（真机验证项攒着一起验）**。

## 一、先按序读（进入状态）
1. **本文件** + `context_memory/家族化专项_决策与进度.md`（★家族化 4 决策/载体路由化架构阻断/P2 落地要点）。
2. `feature/App操作基调_设计系统.md`（★全App家族化统一操作逻辑基调·§三菜品编辑/§四食材编辑/§五质量5阻断/§六分批计划）。
3. `feature/待办总览.md`（**H 节真机bug**含本会话新增崩溃条 + 家族化 XL 项 + F 节反馈批）。
4. `CLAUDE.md` 踩坑红线（透明准则/数据来源/加食材走忌口/营养折算防天价等）。

## 二、工作规则（用户已定·稳定·重要）
1. 中文；**无人值守·全权推进·凡需拍板用推荐方案·快速全做完·不反复问确认**。先 Explore/读代码摸现状再动·别过度设计·动手前验证别改已正确的。
2. **每功能走全套**：门禁（Apple-UX界面 / apple_software_behavior行为 / copywriter文案 / Google代码审查·阻断必修）→存上下文→经验→待办→飞书→commit(`[unattended]`)→push。
3. 🔴构建看输出别信 exit code（`grep BUILD SUCCESSFUL`）。数据bug先 python 拉真机库证实再改。
4. **家族化是必须闭环的专项**（用户2026-07-20强调）：**每页做完即测**；关联页做完做**联动验证**（操作逻辑/界面体验/数据流转）；**全部完成后至少 5 轮回测 + 多方审核**；整体测完才继续其他待办。我侧"测"=构建+shared单测+Google质量审+Apple-UX/行为/文案门禁+逻辑/数据流多角色评审；真机验证按"攒2-3页一批"推git远程发用户验。
5. 健康数据红线、透明准则、联网核准必列数据来源 等（见 CLAUDE.md）。

## 三、本会话交付（均已 push·master 绿·末位 `aaec3a2`）
- `c63bd28` ✅ **修首页推荐图片不符**：NextMealCard 有图优先显该菜真图(StoredImage)、无图回退 emoji；新查询 selectDishImagesByIds + DishRepository.dishImagesByIds + HomeViewModel cachedThumbs + NextDishUi 加图字段。构建绿+单测绿。**真机待验。**
- `c82336c` 🎯 **菜品编辑闪退·真根因修复 + P1 家族化落地**：真机崩溃栈+联网确诊=`ImagePickerButton` coverStyle 分支 **`return@Column`(inline Column 内提前 return)**→组 Start/End 失衡→编辑态封面"空→出图"重组时 `SlotTableKt.key index=-5` 崩(新建态封面恒空不重组故不崩)。**非** bottomBar/无界高度(前两次回退/weight 重做误判·仍崩)。修:改 if/else 去 return@Column + FormBottomBar 加 navBarPadding 开关。P1 家族化(封面前移/保存下移 weight 底栏/步骤折叠)重新落地。**真机待验四链路。**
- `ceed26a` 🛡 **崩溃兜底不闪退**(H0节)：`CrashReporter`抽象(本地占位·后接后台/友盟换 `CrashReporting.reporter`)+`CrashActivity`友好界面(独立`:crash`进程·发送报告/重启/关闭)+`AppLogger` 全局捕获→拉起 CrashActivity+结束进程(不走系统闪退)。**真机待验。**
- `757db5e` 🧩 **家族化 P2 食材编辑页·增量一**：单 moreExpanded 拆 4 独立折叠段(营养数值/更多信息(其它分类)/做法说明/调养建议·各自开合)+去营养大类必选红字+营养 Decimal 键盘+CareRuleEditor 去重复标题+新增 `FoldSection` 助手(if/else 无提前 return·守崩溃红线)。**真机待验。**
- 📚 踩坑沉淀:CLAUDE.md 红线(Compose 禁 inline 内提前 return)+experience/06+**全局经验库 `~/.claude/memory/` 组建为跨技术栈**(新增 android_compose_slottable_early_return.md·MEMORY.md 分类化)。
- 中途历史(回退/据实/决策/载体阻断/进度):`a461b8d`/`44f82a7`/`6193d14`/`be653a3`/`cec8cc5`/`5ca11dd`/`0063848`/`aaec3a2`。

## 四、⏭ 下一步（家族化专项·无人值守推进）
1. ✅ **P1 菜品编辑页**（家族化+闪退根因已修 `c82336c`）·真机待验四链路(编辑既有菜不崩+新建/导入/回传+封面顶/保存底)。崩溃兜底(`ceed26a`)也真机验(制造崩溃看友好界面)。
2. ✅ **P2 食材编辑页·增量一**(`757db5e`)：4 独立折叠段+去必选红字+Decimal 键盘。真机待验。
3. 🔴 **P2 食材编辑页·增量二（接着做这个）**：现状 `picker/IngredientEditorDialogs.kt` 已完成 4 折叠(`FoldSection` 助手·**禁提前 return**)。剩余按 `家族化专项_决策与进度.md §四` + `App操作基调 §四/§五`：
   - **单件克重上移基础区**(仅计件单位条件显·从营养数值段移出) + **别名/图片下沉"更多信息"段**。
   - `EditorSection`→`InsetGroup` 白卡视觉；内联 `TopAppBar`→`AppTopBar`；`confirmDiscard` 自绘守卫→`rememberUnsavedGuard`。
   - **完整 `rememberSaveable` 草稿**(§五阻断③·含 careRules 自定义 Saver)。
   - 剩余 §五阻断：①保存失败错误顶部固定可见 ②保存成功统一反馈(编辑器关闭后由 picker 页 `IngredientPickerScreen` 的 `LaunchedEffect(lastSavedIngredientId)` 发 Snackbar) ⑤图片压缩中禁保存(给 `ImagePickerButton` 加 `onProcessingChange` 外露 processing·禁保存)。
   - **🔴载体路由化(风险块·建议单独一子批·上下文充裕时做)**：全屏 Dialog→路由页。**架构阻断**(见 `家族化专项_决策与进度.md 二`)：`IngredientPickerScreen` 一体两用(食材 Tab 落地页 有 nav / `asDialog=true` 全屏 Compose Dialog 选择器 无 nav 且遮 Snackbar)——Dialog 内无法 push 路由。**推荐**：抽共享 `IngredientEditorContent`；Tab 入口做真路由页(统一 Snackbar)；选择模式内保持原地复用 Content(Snackbar 被遮→Toast)；完全统一需先路由化"选择器本身"。调用点多入口 line 91/152/242/551/596/647 + 搜索/CreateBus。
4. P3 两选择页统一 `SelectionSummaryBar`(F#3) + 餐食编辑对齐 → P4 全App逐批(每批 Apple-UX过·Google审·真机验)。
5. **家族化全部完成后**：≥5 轮回测 + 多方审核(多角色 agent)，整体测完再回其他待办。

> 接手：工作区仅 temp/claude·全 push·master 绿(`aaec3a2`)。构建务必读输出确认 BUILD SUCCESSFUL(`scripts\build-cli.bat :androidApp:assembleDebug`)。**用户远程 git 验证清单**：①菜品编辑不再崩(四链路) ②崩溃出友好界面(不闪退) ③首页推荐显对应菜图 ④食材编辑 4 折叠开合 ⑤之前热量修复。**下一步直接做 P2 增量二**(4 折叠已完成·先做单件克重上移/别名图片下沉/InsetGroup/守卫/草稿/质量阻断，载体路由化留风险子批)。
