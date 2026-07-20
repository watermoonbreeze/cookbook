# 🔖 SESSION 交接入口（新会话先读这里）

> 会话交接唯一固定入口（每次交接覆盖，历史流水在 git）。
> 触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落地文档+覆盖本文件+git 提交。
> 更新时间：**2026-07-20 交接（无人值守·超长会话·P2 食材编辑增量二已本地 commit·待 push）**。androidApp BUILD SUCCESSFUL + shared 单测全绿；Google 质量审无阻断(建议项已修)。**用户授权持续无人值守推进·睡觉中·别中断·完成后继续其他待办**。用户设备暂断→**远程 git 验证（真机验证项攒着一起验）**。

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

## 三、本会话交付
- 🆕 **P2 食材编辑页·增量二**（本地已 commit·待 push）：①单件克重上移基础卡(仅计件单位/已有值显·`WEIGHT_VOLUME_UNIT_NAMES` 按名判)+别名/图片下沉「更多信息」段 ②`EditorSection`→`InsetGroup` 白卡(灰底)/`TopAppBar`→`AppTopBar`/自绘守卫→`rememberUnsavedGuard`/`FoldSection` 各包白卡 ③**完整 `rememberSaveable` 草稿**(文件级 5 个 Saver+`hydrated` 守卫只水合一次·进程被杀恢复不覆盖草稿·`editedN` 集合) ④§五阻断:保存失败错误条顶栏下固定可见/保存成功统一反馈(Tab Snackbar·选择态 Toast 降级)/`ImagePickerButton.onProcessingChange`→压缩中禁保存。构建绿+shared 单测绿+Google 审无阻断(#1 hydrate 竞态/#2 新建残留/#3 savingContinuation 残留/#7 死码/#8 推演时序 均已修)。**真机待验**:编辑既有食材不崩+旋转/进程杀草稿不丢+单件克重条件显+保存反馈。
- 📚 落档:`家族化专项_决策与进度.md`(P2 增量二✅)+`experience/06`(rememberSaveable+hydrated 守卫/Edit 写控制字符坑/按名判计件/InsetGroup 折叠等)。

### 历史（均已 push·master 绿·末位 `aaec3a2`）
- `c63bd28` ✅ **修首页推荐图片不符**：NextMealCard 有图优先显该菜真图(StoredImage)、无图回退 emoji；新查询 selectDishImagesByIds + DishRepository.dishImagesByIds + HomeViewModel cachedThumbs + NextDishUi 加图字段。构建绿+单测绿。**真机待验。**
- `c82336c` 🎯 **菜品编辑闪退·真根因修复 + P1 家族化落地**：真机崩溃栈+联网确诊=`ImagePickerButton` coverStyle 分支 **`return@Column`(inline Column 内提前 return)**→组 Start/End 失衡→编辑态封面"空→出图"重组时 `SlotTableKt.key index=-5` 崩(新建态封面恒空不重组故不崩)。**非** bottomBar/无界高度(前两次回退/weight 重做误判·仍崩)。修:改 if/else 去 return@Column + FormBottomBar 加 navBarPadding 开关。P1 家族化(封面前移/保存下移 weight 底栏/步骤折叠)重新落地。**真机待验四链路。**
- `ceed26a` 🛡 **崩溃兜底不闪退**(H0节)：`CrashReporter`抽象(本地占位·后接后台/友盟换 `CrashReporting.reporter`)+`CrashActivity`友好界面(独立`:crash`进程·发送报告/重启/关闭)+`AppLogger` 全局捕获→拉起 CrashActivity+结束进程(不走系统闪退)。**真机待验。**
- `757db5e` 🧩 **家族化 P2 食材编辑页·增量一**：单 moreExpanded 拆 4 独立折叠段(营养数值/更多信息(其它分类)/做法说明/调养建议·各自开合)+去营养大类必选红字+营养 Decimal 键盘+CareRuleEditor 去重复标题+新增 `FoldSection` 助手(if/else 无提前 return·守崩溃红线)。**真机待验。**
- 📚 踩坑沉淀:CLAUDE.md 红线(Compose 禁 inline 内提前 return)+experience/06+**全局经验库 `~/.claude/memory/` 组建为跨技术栈**(新增 android_compose_slottable_early_return.md·MEMORY.md 分类化)。
- 中途历史(回退/据实/决策/载体阻断/进度):`a461b8d`/`44f82a7`/`6193d14`/`be653a3`/`cec8cc5`/`5ca11dd`/`0063848`/`aaec3a2`。

## 四、⏭ 下一步（家族化专项·无人值守推进）
1. ✅ **P1 菜品编辑页**（家族化+闪退根因已修 `c82336c`）·真机待验四链路(编辑既有菜不崩+新建/导入/回传+封面顶/保存底)。崩溃兜底(`ceed26a`)也真机验(制造崩溃看友好界面)。
2. ✅ **P2 食材编辑页·增量一**(`757db5e`)：4 独立折叠段+去必选红字+Decimal 键盘。真机待验。
3. ✅ **P2 食材编辑页·增量二**(本会话·本地 commit·待 push)：单件克重上移/别名图片下沉/InsetGroup/守卫/完整草稿/§五阻断 全落地+Google 审修完。真机待验。
4. 🔴 **P2 增量二·剩余=载体路由化（下一个·风险子批·上下文充裕时做）**：全屏 Dialog→路由页。**架构阻断**(见 `家族化专项_决策与进度.md 二`)：`IngredientPickerScreen` 一体两用(食材 Tab 落地页 有 nav / `asDialog=true` 全屏 Compose Dialog 选择器 无 nav 且遮 Snackbar)——Dialog 内无法 push 路由。**推荐**：抽共享 `IngredientEditorContent`；Tab 入口做真路由页(统一 Snackbar)；选择模式内保持原地复用 Content(Snackbar 被遮→Toast)；完全统一需先路由化"选择器本身"。调用点多入口 + 搜索/CreateBus。**评估:该项重构大、易砸核心选食材流程——若判定 ROI 低/风险高可跳过留待用户拍板，先推进 P3。**
5. **P3 两选择页统一** `SelectionSummaryBar`(F#3) + 餐食编辑对齐 → P4 全App逐批(每批 Apple-UX过·Google审·真机验)。
6. **家族化全部完成后**：≥5 轮回测 + 多方审核(多角色 agent)，整体测完再回其他待办(`待办总览.md` H 节真机 bug + F 节反馈批)。

> 接手：构建务必读输出确认 BUILD SUCCESSFUL(`scripts\build-cli.bat :androidApp:assembleDebug`)。**用户远程 git 验证清单**：①菜品编辑不再崩(四链路) ②崩溃出友好界面 ③首页推荐显对应菜图 ④食材编辑 4 折叠开合 ⑤热量修复 ⑥**新增:食材编辑家族化(基础卡+4 折叠白卡/单件克重仅计件显/别名图片在"更多信息"/保存成功提示/旋转不丢草稿)**。**下一步:载体路由化(风险子批·可跳)或直接 P3 两选择页统一。**
