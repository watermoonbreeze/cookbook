# 🔖 SESSION 交接入口（新会话先读这里）

> 会话交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落地文档+**覆盖**本文件+git 提交。
> **维护约定（省 token·2026-07-22 定）**：本文件**只保留当前状态**，每次交接**全覆盖**——**不堆"上一次/上上次"交付明细**（目标 ≤1 屏）。**完整历史靠 git**（每次交接一个 commit·`git log`/`git show` 可完整回溯）+ 同目录 **`SESSION_交接_历史.md`**（append-only·每次追加一行：日期·末位 commit·一句摘要）。
> 更新时间：**2026-07-22 · L3+bug+布局+运营方案 多项交付 & 确立"一个 session 一类"规则（全 push origin/master·末位代码 commit `c255aa4`）**。

## 本 session 交付（全 push·按 commit）
- **自建食材 L3 属性双层判定 UI ✅**(`b2f565b`)：shared `FoodAttributeCare.CARE_CODE_TO_NAME`/`expandDeduped`(同病种取更严)/`deriveAttributes`(按唯一 reason 反推属性)+7单测；VM `saveIngredientEditor` 加 `attributeCodes`→展开 `source='attr'` care 与人工合并(人工优先·`distinctBy{categoryId}` 保留 manual 在前)+`guessAttributes`；UI 属性折叠段(`FilterChip`×8)+`AttributeGuessBanner`(按名识别/影响谁/一键清空·可撤)+状态机(`selectedAttrs`/`attrsTouched`/`guessedAttrs`·`Saver`+`hydrated` 守卫)+编辑既有食材反推预勾&过滤 attr 源不进人工 care 列表·submit 由 selectedAttrs 重新展开落库(不丢不重复)+详情标"按属性识别·可改"。门禁全过。方案 `feature/自建食材双层判定_方案讨论.md`(六=规范·已标已实现)。
- **bug 修**(`efdb9cc`)：食材管理页(`selectionMode=false`)新建后误显"选中"高亮→卡片 `selected` 关联 `selecting`(=`selectionMode||composeMode`)·非选择浏览态永不显选中(两处网格渲染点)。
- **编辑器布局优化**(`c255aa4`·用户提)：拍照封面移到表单最上(同菜品·复用 `ImagePickerButton` coverStyle·预设/自建都在顶部)+二级名称与食材名称同一行各占一半;"更多信息"折叠段→只留"其它分类"。守崩溃红线。
- **运营一期 #177 方案**(`e13f2f1`·`apple_operations_designer`)：`feature/运营一期_首屏化方案.md`——盘点发现①首屏化+④周小结页面**已落地**·真正缺口=**③记菜命中慢病结果处轻提示(全新·T1·Snackbar 一次性·陈述事实非判决·仅登记档案触发)**+②首启衔接。P0=③②①·P1=④#178。
- **待办登记本轮批准批次** + 暂缓项归档(待办总览头部) · **确立"一个 session 只做一类事"全局规则**(已落 `~/.claude/CLAUDE.md` 强提醒 + 项目 memory `cookbook-session-by-type`)。
- **🔄 未实现（下一步可接）= A#6-B 当日能量暗因子**：已摸清推荐取数管线；**设计点**——选项B称"当日能量按单成员×share·数据全现成零改动"，但 `RecommendationDataSource.gather` 只有**整菜今日合计**、无"按观看成员折算的今日能量+`CalorieTarget.dailyTarget`"→**需补一处取数·非纯零改动**。方案 `context_memory/算法拍板落地_2026-07-21.md §四`(前置 A2 gate 已落地)。

## 一、先按序读（进入状态）
1. 本文件 + `feature/待办总览.md`（**唯一 backlog 真相源**·头部有 2026-07-22 批准批次与暂缓项）。
2. 对应主题方案文档：`context_memory/算法拍板落地_2026-07-21.md §四`(A#6-B) / `feature/运营一期_首屏化方案.md`(#177) / `feature/自建食材双层判定_方案讨论.md`(L3·已实现) / `feature/健康判定_数值加属性双层.md`+`feature/食材属性标签体系设计.md`(数据双层)。
3. `CLAUDE.md` 踩坑红线 + 架构准则（凡编码必守）。

## 二、工作规则（用户已定·稳定）
1. **🔴 一个 session 只做一类事**（数据/算法/UI/bug/文档）·off-type 的 bug/待办**进队列(`待办总览.md`)不当场做**（除非用户明说"现在做"或真依赖当前上下文）·切类型=换 session。已落全局 CLAUDE.md 强提醒。
2. 中文；**深度模式·无人值守·全权推进·每功能完都验证审核测试(门禁+构建+单测)·快速做完不反复问确认**。先 Explore/读码摸现状·别过度设计·别改已正确的。
3. **每功能走全套门禁**：界面/交互→`apple_ux_designer`(编码前出规范·崩溃敏感区必走)/`apple_visual_designer`；App 自动行为→`apple_software_behavior`；文案→`copywriter`；Android 代码质量→`google_quality_engineer`(阻断必修复复验)·涉架构→`google_architecture_engineer`。
4. 🔴构建看输出别信 exit code（`scripts\build-cli.bat :androidApp:assembleDebug`/`:shared:testDebugUnitTest` grep `BUILD SUCCESSFUL`）。数据 bug 先 python 拉真机库证实再改。崩溃红线：inline Column/Row/Box content lambda 禁 `return@`·coverStyle 变体用 if/else·条件用 item/if 插入式 emit。
5. **每功能一批**：门禁→构建+单测→commit(`[unattended]`)→**push origin/master**→落档。真机验证用户统一做。
6. 红线：健康免责(仅供参考·非医嘱)、透明准则(分级告知)、联网核准必列数据来源、AI 生成内容落库、**热量个人概念(受 CALORIE_NUMBER_ENABLED 开关·不折算显整份)**、抽共享防调参漂移(MealCompositionScorer/ChronicDiseasePenalty 等)。

## 三、⏭ 下一步（**按类型分 session 做**·off-type 进队列）
> **✅ 本 session 已清（全 push）**：L3 属性 UI · 管理页误选中 bug · 编辑器封面上移+名称同行 · 运营 #177 方案 · 待办批次登记 · 会话规则。真机验证待用户做（L3 五步已发飞书）。
>
> **下一个 session 选一类干净开做（交接后 `/clear` → 「会话继续」+ 指定主题）：**
>
> **【数据类】**（联网核准·健康红线·用户把关·可合一个 session）
> - **四项数据核准**：玫瑰花/章鱼/年糕/生蚝(kcal 或蛋白存疑)。数据大轮已部分处理(玫瑰花 30→280 pending·章鱼 135·年糕/生蚝→nlc)，需逐项对**一手权威**复核定 verified/pending·未擅改。
> - **全库健康忌口补漏剩余边界**(F#附2)：核心缺口第一批已闭环，剩 鸡毛蛋卫生忌口 / 鱼油(高胆固醇 vs Omega3) / 植物高嘌呤(干香菇/腐竹/青豆) avoid 还是 limit·联网核准后定·来源入数据来源页。
>
> **【算法类】**（独立 session·需先定口径）
> - **A#6-B 当日能量暗因子**(选项B·greenlit)：⚠️先定"当日能量按单成员×share"取数(gather 现无·需补 `CalorieTarget.dailyTarget`+今日成员能量)·`HealthRuleEngine` 加同构软因子(仿 chronicDiseaseNutrition·封顶≈0.4·暗因子不可见·缺数据/多成员/没吃多恒0)·守热量个人概念红线。方案 `算法拍板落地_2026-07-21.md §四`。
> - A#4 GI 一维(低价值·可不做)。
>
> **【UI/交互类】**（各走门禁·多需用户在场）
> - **运营一期 #177 落地**：P0=③记菜命中慢病轻提示(全新·Snackbar 一次性)+②首启衔接·须过 apple_software_behavior+apple_ux_designer+copywriter。
> - **#203 食材编辑器 Dialog→路由页**(用户在场·高爆炸半径)。
> - **#208 更新基础数据启动弹窗**(待有数据变更才验)。
>
> **【真机待验】**(用户)：L3 属性 UI(飞书 5 步) · 管理页选中修复 · 编辑器封面/同行布局。
>
> **【暂缓·时机成熟】**：营养表体现营养素+8属性列+冻结左列(食材体系落地后·需 UX 门禁) · 数据性能专项(数据源增多) · nlc 匹配迭代提命中(可选)。
>
> **接手先读**：`待办总览` + 本档 + 对应主题方案文档。每功能过门禁+构建+单测+push·真机验证用户统一做。
