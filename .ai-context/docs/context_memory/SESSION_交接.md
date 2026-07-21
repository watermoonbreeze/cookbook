# 🔖 SESSION 交接入口（新会话先读这里）

> 会话交接唯一固定入口（每次交接覆盖，历史流水在 git）。
> 触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落地文档+覆盖本文件+git 提交。
> 更新时间：**2026-07-21 交接（超长无人值守 session·17 提交全 push origin/master 末位 `4cf9ddb`）**。本轮：D1-D5 确定方案全落地(D3去红/D4周计划慢病软降/D2卡化/D1-1 CookMode+D1-2 FamilyEdit VM化) + 用户新拍板算法批(B#6/C#F1/C#F2) + **周计划营养线全链路(domain 一期 + AiPlan 概览卡)** + **AiPlan 界面统一 P1** + **推荐带营养素热量** + 拍照UI改版&查看器bug修 + 报告空周跳周计划。**UI 全部做完用户统一真机测(深度模式·每功能过门禁)。**

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

## 三、本 session 交付（17 提交·全 push·末位 `4cf9ddb`）
- **确定方案 D 批**：D3 膳食均衡度色去红(收敛 nutritionLevelColor) · D4 周计划慢病 GI/嘌呤软降(抽 ChronicDiseasePenalty 与单餐同口径) · D2 AiSettings 卡化 · D1-1 CookMode + D1-2 FamilyEdit **VM 化(F-Arch2/3 结案·min-fix)**。
- **算法拍板批**：B#6 recommend 封顶 · C#F1 强版模型输出后补组合缺口 · C#F2 早餐软硬透传单餐(避免白粥+豆浆两软无蛋)。均单测+Google 审。
- **🌟周计划营养线全链路**：domain 一期(`WeeklyNutritionLineAggregator`+`NutritionLineAdvisor`+8 单测·结构层) + **AiPlan「一周营养搭配」概览卡**(把整周结构覆盖/缺口/均衡度盛出来·去红·免责)。
- **🌟AiPlan 界面统一 P1**：DayCard 白卡化 + 控件折叠进「计划设置」弹层 + 提示条独立 + 空态 EmptyState + 标注去 emoji(§9.35)。
- **🌟推荐带营养素热量**：AiRecommend 每菜"整份约 X 千卡·蛋白/脂肪/碳水"行 + 组合卡合计 + 钠"偏咸"提示 + 缺数据"待完善"·热量受开关(§9.36)·规则+模型+兜底三路统一·`DishNutritionLine`/`rememberCalorieNumberEnabled`。
- **拍照**：删除入口迁全屏查看器(去缩略图角标免误触·单图铺开·§9.34) + **查看器删当前图预览不刷新 bug 修**(produceState key preview 恒 null·用户真机抓的)。
- **报告空周/月→跳一周计划**(带周日期·月含月首日·`WEEK_PLAN_ROUTE` 参数化)。
- 会商方案：营养线四角色(产品/营养师/运营/文案) · AiPlan界面统一 · A#4/A#6 得失分析。

## 四、⏭ 下一步（剩余 greenlit 队列·深度模式逐个门禁）
> 推荐顺序（价值×独立性）：
1. **推荐带营养素热量·AiPlan DayCard follow-up**：AiRecommend 已做·把 `DishNutritionLine` 复用到 AiPlan 逐日卡每菜(数据从计划 dishNutrition·同款)。小·独立。
2. **AiPlan 界面统一 P2·营养线概览卡视觉**：`apple_visual_designer` 定色阶(色点/胶囊/深色 α·概览卡已在·仅打磨)。或先跳。
3. **菜品编辑页低频内容分类折叠·与食材编辑统一**（用户提·需 `apple_ux_designer` 门禁·关联 F#选择编辑统一 P2）。
4. **WeekPlan 屏营养线**（"两屏"第二屏·数据源=已排 MealRecord·从已排餐反查主料名走同一 `WeeklyNutritionLineAggregator`）。
5. **A#6 B 当日能量软因子**：需先做 **A2 孕期/哺乳不评热量 gate**(C组9)→再 B(同构 chronicDiseaseNutrition·封顶0.4·暗因子·复用 CalorieTarget)。
6. **A#4 GI 一维**：需先理清 `gi_high`(ingredients categories 值·21食材)的 code→category_id 解析路径(食材按分类查·food_category 无 code 列)。或用 DishCandidate 补 highGiStaple 进 MMR(分析结论：最小化做或可不做·中偏低价值)。
7. **CookingTimer VM化**(F-Arch1·D1 末页·真机关键·0 VM+倒计时/AlarmManager/息屏·androidApp 无测试基建须真机验计时+持久化)。
8. **数据扩充**(营养因子/菜品/食材/营养素·大批量联网核准·分批即写即存+来源入数据来源页)。
9. 小 follow-up：热量开关 calorieOn 上提减订阅(perf)。

> **本轮暂缓/待用户**：C#F2 载体已做·营养线宏量慢病分布层(需 NutritionTotals)·补充建议回灌 PeriodPlanner 闭环。
> **接手推进**：读 `算法拍板落地` + `待办总览` 掌握全貌 → 按上序做 → 每功能过门禁+构建+push。真机验证用户统一做。
