# 🔖 SESSION 交接入口（新会话先读这里）

> 会话交接唯一固定入口（每次交接覆盖，历史流水在 git）。
> 触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落地文档+覆盖本文件+git 提交。
> 更新时间：**2026-07-21 续接 session·营养线 UI 主线 + A2 健康红线 + 报告小修（多笔全 push origin/master·末位 `48ed37a`）**。承上一 session(17 提交末位 `4cf9ddb`)。本轮：
> **营养线三件套**：#1 AiPlan 逐日卡每菜营养行 `9952c25` + #3 菜品编辑低频区分类折叠 `da85c1a` + #4 WeekPlan 已排周营养概览卡 `4ac74bf`(抽/复用共享 DishNutritionLine/FoldSection/NutritionLineCard·单一真相源·#4 修1阻断空周0分卡已复验)。
> **A2 健康红线 `3452004`**：孕期/哺乳期成员不评估热量(复用已拍板「生命阶段」care 信号·**不新建字段**·集中式 gate:BodyMetrics.skipCalorieEval@Transient+FamilyMember.isCalorieExempt→toBodyMetrics→dailyTarget/referenceKcal 首部 return null·5个热量点零改动全覆盖·数据层加性别 gate·UI 仅女性显孕哺+切男性清理+说明+MemberCard中性提示·+3单测·Google审无阻断)。
> **报告结构日历 F#5 `b45fd46`**(用户今日提+拍板"两排")：结构日历改**简单两排**(周单行7格/月均分两排带小日号)+图例4档+没记空心+去红级别色+日号明度取色。先 FlowRow 快修(`343ebd3`)→用户确认后升级(周对齐网格版已过 Google 审→按用户偏好简化为两排·避月首日周中对齐复杂)。
> **#2 概览卡视觉打磨判跳过**(现状复用 §9.35 达标)。均走门禁+构建+push。**UI 待用户统一真机测。**
>
> **📌 数据扩充(联网核准营养)已泊车待跑**：见 `数据扩充_营养核准_待跑.md`——195 条 pending 缺口已摸清+分 5 片+agent prompt+合并步骤全就绪，用户要求"先做 UI 后跑数据"，**下次要数据扩充直接按该档执行不用重摸**。

## 一、先按序读（进入状态）
1. **本文件** + `context_memory/算法拍板落地_2026-07-21.md`（8 项算法拍板状态总表 + A#4/A#6 分析 + C#F2/C#F3 结论）。
2. `feature/待办总览.md`（**唯一 backlog 真相源**·A 类新增营养线/推荐带营养素/报告空周/菜品编辑折叠/AiPlan风格统一等·各项状态最新）。
3. `feature/周计划营养线方案.md`（营养线四角色会商·§⏱落地进度：domain✅+AiPlan概览卡✅·WeekPlan屏+宏量慢病层 follow-up）+ `feature/AI推荐界面统一方案.md`（P1✅ 已实现·P2 概览卡随营养线）。
4. `feature/苹果风格UI设计方案.md` **§9.34/9.35/9.36**（本轮新范式：全屏查看器含删除 / AI推荐三档家族化统一 / 推荐营养呈现）。
5. `CLAUDE.md` 踩坑红线 + 架构准则（凡编码必守）。

## 二、工作规则（用户已定·稳定）
1. 中文；**深度模式·无人值守·全权推进·每做一个功能完都验证审核测试(门禁+构建+单测)·快速做完不反复问确认**。先 Explore/读码摸现状再动·别过度设计·动手前验证别改已正确的。
2. **每功能走全套门禁**：界面/交互→`apple_ux_designer`(编码前出规范·崩溃敏感区必走)/`apple_visual_designer`；App 自动行为→`apple_software_behavior`；文案→`copywriter`；**Android 代码质量→`google_quality_engineer`(阻断必修复复验)·涉架构→`google_architecture_engineer`**。会诊类多角色并行→我汇总收敛落方案文档。
3. 🔴构建看输出别信 exit code（`scripts\build-cli.bat :androidApp:assembleDebug` / `:shared:testDebugUnitTest` grep `BUILD SUCCESSFUL`）。数据 bug 先 python 拉真机库证实再改。崩溃红线：inline Column/Row/Box content lambda 禁 `return@`(SlotTable 崩)·coverStyle 变体用 if/else·条件用 item/if 插入式 emit。
4. **每功能一批**：门禁→构建+单测→commit(`[unattended]`)→**push origin/master**(用户要真机验)→落档。真机验证用户统一做(UI 全做完)。
5. 红线：健康免责(仅供参考·非医嘱)、透明准则(分级告知)、联网核准必列数据来源、AI 生成内容落库、**热量个人概念(受 CALORIE_NUMBER_ENABLED 开关·不折算显整份)**、抽共享防调参漂移(MealCompositionScorer/ChronicDiseasePenalty/DishNutritionLine 等)。

## 三-新、本续接 session 交付（3 提交·全 push·末位 `4ac74bf`）
- **#1 AiPlan 逐日卡每菜营养行 `9952c25`**：抽 `DishNutritionLine`(+`DishNutritionUi`+`toDishNutritionUi`)到 `ui/component` 共享(原埋 AiRecommend 私有)，AiPlan 逐日卡每菜显整份热量+宏量(受开关)+钠偏咸+缺数据待完善；AiPlanVM 加 NutritionRepository 批量查(distinct·无 N+1·runCatching 兜底)。
- **#3 菜品编辑低频区分类折叠 `da85c1a`**：抽 `FoldSection` 到 `ui/component` 共享(原私有于 IngredientEditorDialogs)；NewDishScreen 低频区从单个 MoreOptionsHeader 大折叠→两个 `InsetGroup{FoldSection{}}`(操作步骤/更多信息·各自开合)；折叠态 `rememberSaveable(editingId)`(新建收起/编辑展开/有内容自动展开)；删孤儿组件 MoreOptionsHeader。
- **#4 WeekPlan 已排周营养概览卡 `4ac74bf`**：抽 `NutritionLineCard` 到 `ui/component` 共享(原私有于 AiPlanScreen)；WeekPlanVM 从已排卡片主料名聚合整周营养线(同 `WeeklyNutritionLineAggregator`)；LazyColumn 首项插概览卡。Google 审修 1 阻断(空周 dayCount 恒=窗口天数≠有排菜天数→改 VM 侧有真实主料数据才置 nutritionLine 否则 null·已复验)。
- **#2 概览卡视觉打磨=跳过**：现状复用 §9.35 去红色阶(`nutritionLevelColor` 琥珀→绿)+主题自适应文字·已达标·无边际价值。
- 门禁贯彻：每功能 Explore 摸现状→(交互/视觉/质量)门禁→构建(daemon 偶瞬崩·重试即过·非编译错)→单测(改动纯 androidApp UI 层·shared 未触)→commit→push。

## 三、上一 session 交付（17 提交·全 push·末位 `4cf9ddb`）
- **确定方案 D 批**：D3 膳食均衡度色去红(收敛 nutritionLevelColor) · D4 周计划慢病 GI/嘌呤软降(抽 ChronicDiseasePenalty 与单餐同口径) · D2 AiSettings 卡化 · D1-1 CookMode + D1-2 FamilyEdit **VM 化(F-Arch2/3 结案·min-fix)**。
- **算法拍板批**：B#6 recommend 封顶 · C#F1 强版模型输出后补组合缺口 · C#F2 早餐软硬透传单餐(避免白粥+豆浆两软无蛋)。均单测+Google 审。
- **🌟周计划营养线全链路**：domain 一期(`WeeklyNutritionLineAggregator`+`NutritionLineAdvisor`+8 单测·结构层) + **AiPlan「一周营养搭配」概览卡**(把整周结构覆盖/缺口/均衡度盛出来·去红·免责)。
- **🌟AiPlan 界面统一 P1**：DayCard 白卡化 + 控件折叠进「计划设置」弹层 + 提示条独立 + 空态 EmptyState + 标注去 emoji(§9.35)。
- **🌟推荐带营养素热量**：AiRecommend 每菜"整份约 X 千卡·蛋白/脂肪/碳水"行 + 组合卡合计 + 钠"偏咸"提示 + 缺数据"待完善"·热量受开关(§9.36)·规则+模型+兜底三路统一·`DishNutritionLine`/`rememberCalorieNumberEnabled`。
- **拍照**：删除入口迁全屏查看器(去缩略图角标免误触·单图铺开·§9.34) + **查看器删当前图预览不刷新 bug 修**(produceState key preview 恒 null·用户真机抓的)。
- **报告空周/月→跳一周计划**(带周日期·月含月首日·`WEEK_PLAN_ROUTE` 参数化)。
- 会商方案：营养线四角色(产品/营养师/运营/文案) · AiPlan界面统一 · A#4/A#6 得失分析。

## 四、⏭ 下一步（营养线 UI + A2 健康红线已收官·剩余项各有约束）
> ✅ 本续接 session 已做完：#1 AiPlan逐日营养行 · #3 菜品编辑折叠 · #4 WeekPlan营养卡 · **A2 孕哺不评热量(健康红线·`3452004`)** · 报告月历改两行(`343ebd3`) · backlog 状态对账。#2 视觉打磨判跳过。
>
> 剩余队列（各有前置/性质约束·多数需真机或用户定向）：
1. **A#6-B 当日能量软因子**：A2 gate 已落地(前置解锁)→可做 B(同构 `chronicDiseaseNutrition`·封顶0.4·**暗因子·用户不可见**·复用 CalorieTarget·缺数据恒0)。⚠️但"暗因子不可验证"且与"热量个人概念"红线有张力(handoff 建议 A 现在做/B 排期·**未显式拍板**)——**建议排期时先与用户确认是否要**,勿盲上。
2. **A#4 GI 一维**（低价值·可不做）：`gi_high`(21食材) code→category_id 解析路径未清(food_category 无 code 列)。分析结论=最小化做或不做。
3. **CookingTimer VM化**(F-Arch1·**需真机验**)：CookMode 末页 0 VM·含倒计时/AlarmManager/息屏·androidApp 无测试基建→无人值守做有回归风险·**建议用户在场时做**。
4. **数据扩充**(联网核准营养·**已泊车**)：用户要求"先UI后数据"·UI 主线现已largely done·直接按 `数据扩充_营养核准_待跑.md` 执行(195 pending 已分片·后台+断点续连)。**可无人值守安全推的主力。**
5. 其余 backlog：190 选择/编辑统一 P1P2(需真机)、193 AI规则推荐贴合(🔴·algorithm会诊剩下批)、203 全App家族化 P2-P4(需真机验P1)、204 F#3底部栏展开(需UX)。
> **F#5 已完成**(`b45fd46`·两排版)——原"下一步#4"结案。

> **接手推进**：读 `算法拍板落地` + `待办总览`(单一真相源) + 本档 → 剩余多数需真机/用户定向;可无人值守安全推的主要是**数据扩充(泊车档·后台跑)**。A#6-B 暗因子建议先确认。每功能过门禁+构建+push·真机验证用户统一做。
