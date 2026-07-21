# 🔖 SESSION 交接入口（新会话先读这里）

> 会话交接唯一固定入口（每次交接覆盖，历史流水在 git）。
> 触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落地文档+覆盖本文件+git 提交。
> 更新时间：**2026-07-21 交接（超长无人值守 session·全部 push origin/master `48e601d`）**。本轮：推荐算法打磨 3 轮 + 慢病知情引导 + 拍照回归修复 + **架构准则精修(数据驱动两条正交)+Google×Apple 跨平台评估** + 全局架构审计+F1 + 家族化 P4 色收敛。**用户已确定 D1–D5 方案(见下 ⏭)，正去真机验证，授权我按确定方案继续。**

## 一、先按序读（进入状态）
1. **本文件** + ★`context_memory/SESSION_2026-07-21_验证与决策索引.md`（**本轮总索引**：要用户确定的方案 D1–D6 + 真机验证 V1–V6 + 产出文档索引·用户正逐个看）。
2. `context_memory/数据驱动UI架构审计_2026-07-21.md`（全局架构审计 findings + 整改路线·F1 已修·无 VM 页专项 backlog）。
3. `feature/跨平台架构策略.md`（架构评估结论·数据驱动准则两条正交·iOS/鸿蒙/桌面路线）+ `CLAUDE.md`「架构与代码质量准则」（精修版·**凡编码必守**）。
4. `feature/推荐算法深挖分析.md`（算法 3 轮打磨+"剩余见底"）+ `context_memory/待拍板队列.md`（算法需拍板项）。
5. `CLAUDE.md` 踩坑红线（含本轮升级：State 重建源头兜底/Screen 不注入 Repository/inline 内禁提前 return 等）。

## 二、工作规则（用户已定·稳定）
1. 中文；**无人值守·全权推进·凡需拍板用推荐方案·快速做完·不反复问确认**。先 Explore/读码摸现状再动·别过度设计·动手前验证别改已正确的。
2. **每功能走全套门禁**：界面/交互→`apple_ux_designer`/`apple_visual_designer`；App 自动行为→`apple_software_behavior`；文案→`copywriter`；**Android 代码质量→`google_quality_engineer`（阻断必修复复验）、涉架构→`google_architecture_engineer`**；iOS→`apple_architect`/`apple_quality_engineer`（见架构准则归口）。
3. 🔴构建看输出别信 exit code（`scripts\build-cli.bat :androidApp:assembleDebug` grep `BUILD SUCCESSFUL`）。数据 bug 先 python 拉真机库证实再改。**崩溃敏感区(coverStyle/inline 布局)守组结构平衡·禁 return@Column**。
4. **数据驱动界面准则**（本轮确立·CLAUDE.md）：UI=State 纯函数·VM 单一真相源 UiState·薄 VM·业务下沉 shared·Screen 不注入 Repository·重建 State 用 prev 源头兜底粘性字段。
5. 健康免责红线、透明准则(分级告知·诚实不操纵)、联网核准必列数据来源、AI 生成内容都落库(营养估算标待核) 等（见 CLAUDE.md + 记忆）。
6. **每批**：门禁→构建+单测→commit(`[unattended]`)→push→落档→（真机项攒给用户）。真机回测须用户在场。

## 三、本 session 交付（均已 push·master 末位 `48e601d`）
- 🍎 **推荐算法打磨 3 轮 9 笔**（每轮过 Google 门禁·第 1 轮抓到并修了我引入的真 bug）：RANDOM 999 崩溃修复·`MealCompositionScorer` 抽取·MMR 重油族·一餐主料不重复·组合完整性喂 prompt·周计划重油去重·prompt 因子透传(重油度+互补分级)·diversify 防御化·护栏测试。会诊结论"安全自主项见底"(其余需拍板·见 D4/D5)。
- 🩺 **慢病知情引导(F4b·用户拍板 b)**：已登记痛风/糖尿病+非偏营养→推荐页顶部一次性可关闭横幅"切偏营养=高GI/嘌呤菜靠后"。设计门禁+Google 审无阻断。守透明/诚实不操纵/免责。
- 📷 **拍照回归修复**：菜品/食材编辑"3 张变 1 张"根因修复+封面 16:9+strip 多图+center_crop+崩溃红线审通过。§9.32 沉淀。
- 🏛 **架构（用户重点）**：数据驱动准则**精修为两条正交**(UDF UI层·MVVM+UDF 措辞澄清 / 共享边界·共享逻辑原生 UI)+Google×Apple 跨平台会诊(`跨平台架构策略.md`·含鸿蒙/桌面路线·"薄共享·VM 保持薄"结论)+Android→Google/iOS→Apple 归口。
- 🔬 **全局架构审计**：数据驱动符合度中偏高/质量高+**F1 mapResult 源头兜底修复**(Google 审无阻断·还修了空态潜在 bug)+F3 currentSlot 可测+红线升级。无 VM 页专项(D1)入 backlog。
- 🎨 **家族化 P4 色收敛**：宏量/提示/收藏星色→单一来源 token(防漂移)+文档页不卡化边界(§9.33)。
- 📑 落档：`SESSION_2026-07-21_验证与决策索引.md`(总索引)+审计/架构策略/深挖/待拍板/设计方案 §9.31–9.33 等。

## 四、⏭ 下一步（用户已确定 D1–D5·真机验证期间按此继续）
> 详见 `SESSION_2026-07-21_验证与决策索引.md`。用户 2026-07-21 拍板结果：
1. **D1 无 VM 页 VM 化·按顺序全做**(CookingTimer→FamilyEdit→CookMode)：把业务态+数据访问从 Composable 抽进 ViewModel(UDF 越层修复)。**CookingTimer 含倒计时/AlarmManager/息屏恢复·风险最高·必真机验计时+持久化**(androidApp 无测试基建)。逐页过 Google 架构+质量审。正向样板 `IngredientPickerViewModel`/`NewDishViewModel`。
2. **D2 AiSettings「模型来源」组 InsetGroup 卡化·做**：✅**交互规范已产出(交接时 `apple_ux_designer` 已回·直接实现)**：单 `InsetGroup(title="模型来源")` 包 3 个 RuntimeRow(radio 行改 list-row·`padding(horizontal=16,vertical=14)`·整行 selectable·端侧 disabled)+各自展开子块(CloudSection/自测·外层 `padding(start=48,end=16,bottom=14)` 对齐文字左缘 48dp)·行间 `InsetDivider(startIndent=48)`(端侧末行后不加)；`Scaffold(containerColor=background 灰底, contentWindowInsets=0, topBar=AppTopBar)`·外层 Column 去 `.padding(16)` 加 verticalScroll；隐私小字移**卡外页脚**(裸 Text·`padding(horizontal=16,top=12)`)。参照 `FeatureSettingsScreen` 用法。**崩溃红线**:条件渲染 CloudSection/自测用 if 插入(安全)·禁 content lambda 内 `return@InsetGroup`/任何 early return·变体行用 if/else。单文件 `AiSettingsScreen.kt`·无 VM/逻辑改。→ 实现后 Google 审。
3. **D3 DietReport 膳食均衡度色去红·做**：现 `DietReportScreen.levelColor`(L349) 0=红-alarm、结构 legend(L382) 也硬编码→收敛到**色系墙 gentle 色系**(`NutritionColor.nutritionBase` 绿/浅绿/黄绿/琥珀/灰·去红·合"不制造焦虑"健康准则)。纯色值·需暴露 `nutritionBase` 为公开或加 `nutritionLevelColor(level)`。真机验 DietReport/色系墙一致。
4. **D4 周计划 GI/嘌呤软降·做**：单餐 `HealthRuleEngine` 已有慢病数值软降(仅偏营养风格)+知情引导，`PeriodPlanner` 无→给 `PlanDish` 补 GI/嘌呤命中字段(gatherForPlan 扩查·复用 `dishQualitativeHits`)+同口径开关(仅偏营养/登记病种)。与单餐口径一致。
5. **~~D5 无库存兜底~~ → 核实结论=非真 gap·已覆盖(交接时 `Explore` 已回)**：`RandomMode` 即无库存入口(全库食材当"在手"·`onHandNonSeasoning.isEmpty()` 剔除**不触发**·不空不退化)；库存空→PANTRY 空态"库存里还没有能用到的食材"→用户手动切"随机推荐"→全库推荐(忌口/限量/慢病软约束全生效)；库存挂钩关时 PANTRY 不可达强制 RANDOM。**原设想的 `requireOnHand` 重构无必要**。**仅剩可选小 UX**(库存空时自动提示"切随机?"·非必需·体验加分)——**降级为可选打磨·D5 实质不做**(除非用户要那个 auto-prompt)。→ D5 从队列移除,腾给 D1/D2/D3/D4。

> **本轮暂缓(需真机/决策/门禁)**：D2 剩载体路由化、D6 AI 生成内容落库功能(放开AI限制/AI对话·一期稳定后)、待拍板队列其余算法项(菜级营养字段/当日能量/validate补菜/recommend封顶/早餐软硬透传)、项5 卡语言统一、D5 库存空 auto-prompt(可选)。
> **接手推进顺序**：读 `SESSION_2026-07-21_验证与决策索引.md` 掌握全貌 → **D3(快·纯色去红)→D4(算法·周计划GI软降)→D2(规范已就绪·直接实现+Google审)→D1(大专项·CookingTimer→FamilyEdit→CookMode·CookingTimer 真机关键)**。构建务必 grep `BUILD SUCCESSFUL`。**真机验证 V1–V6 用户自行做**。
