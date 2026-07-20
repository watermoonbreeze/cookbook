# 🔖 SESSION 交接入口（新会话先读这里）

> 会话交接唯一固定入口（每次交接覆盖，历史流水在 git）。
> 触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落地文档+覆盖本文件+git 提交。
> 更新时间：**2026-07-20 交接（无人值守·超长会话·全 push origin/master 到 `c63bd28`）**。androidApp BUILD SUCCESSFUL + shared 单测全绿；工作区仅 temp/claude。**用户授权持续无人值守推进·每完成一项 commit+push+飞书**。用户设备暂断/外出→**远程 git 验证**。

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

## 三、本会话交付（均已 push·master 绿·末位 `ceed26a`）
- `c63bd28` ✅ **修首页推荐图片不符**：NextMealCard 有图优先显该菜真图(StoredImage)、无图回退 emoji；新查询 selectDishImagesByIds + DishRepository.dishImagesByIds + HomeViewModel cachedThumbs + NextDishUi 加图字段。构建绿+单测绿。**真机待验。**
- `c82336c` 🎯 **菜品编辑闪退·真根因修复**：真机崩溃栈+联网确诊=`ImagePickerButton` coverStyle 分支 **`return@Column`(inline Column 内提前 return)**→组 Start/End 失衡→编辑态封面"空→出图"重组时 `SlotTableKt.key index=-5` 崩(新建态封面恒空不重组故不崩)。**非** bottomBar/无界高度(前两次回退/weight 重做误判·仍崩)。修:改 if/else 去 return@Column + FormBottomBar 加 navBarPadding 开关。P1 家族化(封面前移/保存下移 weight 底栏/步骤折叠)重新落地。**真机待验四链路。**
- `ceed26a` 🛡 **崩溃兜底不闪退**(H0节)：`CrashReporter`抽象(本地占位·后接后台/友盟换 `CrashReporting.reporter`)+`CrashActivity`友好界面(独立`:crash`进程·发送报告/重启/关闭)+`AppLogger` 全局捕获→拉起 CrashActivity+结束进程(不走系统闪退)。**真机待验。**
- 📚 踩坑沉淀:CLAUDE.md 红线(Compose 禁 inline 内提前 return)+experience/06+**全局经验库 `~/.claude/memory/` 组建为跨技术栈**(新增 android_compose_slottable_early_return.md·MEMORY.md 分类化)。
- 中途历史(回退/据实/决策/载体阻断):`a461b8d`/`44f82a7`/`6193d14`/`be653a3`/`cec8cc5`/`5ca11dd`。

## 四、⏭ 下一步（家族化专项·无人值守推进）
1. ✅ **P1 菜品编辑页家族化+闪退根因已修(`c82336c`)·真机待验四链路**：编辑既有菜(原崩点)不崩+新建/导入/回传+封面顶部/保存底部。**验过=P1 家族化参考页确立。** 崩溃兜底(`ceed26a`)也真机验一下(制造崩溃看友好界面)。
2. 🔴 **家族化 P2 食材编辑页**（可推进·崩因不在共享件：现有食材编辑器本就用 ImagePickerButton+moreExpanded 折叠且未报崩）。按 `家族化专项_决策与进度.md §四` + `App操作基调 §四/§五`：
   - 现状 `picker/IngredientEditorDialogs.kt` 的 `IngredientEditorDialog`(全屏 Dialog·单 moreExpanded)；调用点在 `IngredientPickerScreen.kt`(create/edit 两处 + 多入口 line 91/152/242/551/596/647)。
   - 目标：基础卡(名称*+营养大类chip+单位+单件克重[仅计件条件显·从营养区上移]) → 4 独立折叠段(营养数值/更多信息[别名/图片/其它分类]/做法说明/调养建议) → InsetGroup 白卡 → 完整 rememberSaveable 草稿(含 careRules 自定义 Saver) → rememberUnsavedGuard → 底部 FormBottomBar。
   - 质量5阻断(§五)：①保存失败错误顶部固定可见 ②保存成功统一反馈(编辑器关闭后由 picker 页发 Snackbar) ③完整草稿 ④克数 remember(key) ⑤图片压缩中禁保存(给 ImagePickerButton 加 `onProcessingChange` 外露 processing) + 营养 KeyboardType.Decimal。
   - **载体阻断**(见决策文档二)：Tab 入口可做真 nav 路由页(统一 Snackbar)；选择模式(全屏 Dialog 内)保持原地复用 Content(Snackbar 被遮→Toast)；完全统一需先路由化"选择器本身"→拆紧随子批。**建议本批先做"抽共享 IngredientEditorContent + 4折叠 + InsetGroup视觉 + 质量阻断 + 完整草稿"，载体 route 化作为 P2 第二子批。**
3. P3 两选择页统一 `SelectionSummaryBar`(F#3) + 餐食编辑对齐 → P4 全App逐批(每批 Apple-UX过·Google审·真机验)。
4. **家族化全部完成后**：≥5 轮回测 + 多方审核(多角色 agent)，整体测完再回其他待办。

> 接手：工作区仅 temp/claude·全 push·master 绿(c63bd28)。构建务必读输出确认 BUILD SUCCESSFUL(`scripts\build-cli.bat :androidApp:assembleDebug`)。用户远程 git 验证：菜品编辑不再崩 + 首页推荐显真菜图 + 之前热量修复。P1 家族化重做等真机崩溃栈。
