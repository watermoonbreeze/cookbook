# 🔖 SESSION 交接入口（新会话先读这里）

> 会话交接唯一固定入口（每次交接覆盖，历史流水在 git）。
> 触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落地文档+覆盖本文件+git 提交。
> 更新时间：**2026-07-22 无人值守·数据扩充第二批 + 全数据源脚本交叉核对 + 资产化 + cron（4 笔全 push origin/master·末位=资产化批）**。承上一 session(营养线 UI 等·末位 `6a08ecf`)。本轮全程**用户拍板"脚本优先不烧token"**：
> **① 第二批数据扩充(脚本非LLM联网·省token)**：satFat+122(curl下载USDA SR Legacy全库13MB→建7793索引→中文映射本地匹配) / purine+31(用户授权·忌口红线已核与现有加工肉口径一致) / gi+5(仅补有标准GI主食)。单测过·来源已入数据来源页。见 `数据扩充_营养核准_待跑.md`(已记第二批完成)。
> **② 全数据源脚本交叉核对(零token全量·用户诉求)**：能量Atwater自洽(本地) + USDA全库交叉 + nlc中国口径按名交叉(逆向出foodName接口·502条)。**三源真录入错铁证仅1条=速冻虾仁kcal199→89(已修)**；USDA 28条差异几乎全是中/美口径差异非错(不盲从他源);nlc精确匹配消错配噪音。见 `全数据源交叉核对_发现.md`。
> **③ 资产化+cron(用户"更优建议")**：`scripts/data/` 工具集(nutri_selfcheck.py能量自洽 + nlc_cross.py同口径交叉 + cn_usda_map/cn_gi_purine_ref数据资产 + README含nlc接口备忘) + cron `22be4982` 月度体检(每月1号·跑脚本+待核飞书+只报告不改库)。
> **🟡 4项待核交用户把关(守健康数据人工把关红线·未擅改)**：玫瑰花kcal30(与自身PFC矛盾) / 章鱼kcal135(偏高) / 年糕kcal348(口径存疑) / 生蚝蛋白5.3(偏低)。
> **④ 健康判定=营养数值+食材属性双层(用户确立·参照啤酒·已固化)**：已固化 CLAUDE.md门禁红线(第120行) + 方案文档 `feature/健康判定_数值加属性双层.md`(整体审视矩阵/缺口/方案A渐进-B属性标签体系-C)。含糖/高果糖属性care首批:可乐/白糖/红糖/冰糖/甘蔗/蜂蜜/葡萄干/蔓越莓干/红枣 补痛风limit+糖尿病(可乐avoid)·care600。**🔴联网纠正**(用户"联网看具体情况"):食养指南2024新鲜水果与痛风无显著相关→**撤销荔枝/香蕉/芒果/西瓜痛风care**(原过度)·限的是加工浓缩果糖(含糖饮料/果汁/果葡糖浆/果脯蜜饯)。番茄酱等调味品"适量就行"不补。**页面同步已做**:健康状态参考页(判定机制节+痛风/糖尿病属性节+新鲜水果口径)、食材详情页(数据驱动自动·care经seed显示宜忌+红绿灯·无需改码)。单测+构建过·push。**用户"都要A+B"已做**：A补缺口(排查发现高血脂数值层satFat/chol已覆盖·反式脂肪食材不在库·数值已判红加care limit无效→无有效补漏·核心由含糖类体现) + **B属性标签体系首版**(`FoodAttribute`枚举+`FoodAttributeCare`声明式映射+`ingredient_attributes.json`打标+seed展开去重人工优先·**向后兼容现有care零变化**·未来加食材打标签自动配全care·7用例单测·SEED_LOGIC v8→v9·设计见 `feature/食材属性标签体系设计.md`)。**规则优化**(用户要求·果糖-水果教训):脚本方案加**三-B「判定口径必联网核实」**——数值抓取脚本零token,但属性→忌口映射/阈值/机制是"规则"·必联网核实权威指南别想当然(核实边界)·CLAUDE.md+属性门禁同步。反式脂肪/高草酸待对应食材入库/档案。详见 `属性风险care补漏.md`、`健康判定_数值加属性双层.md`。
>
> **📌 剩余可自主的数据优化(可选)**：nlc按名匹配 miss239(太严漏糙米/猪瘦肉等)可迭代"归一+词根映射"提命中·让月度体检更全。非紧急。

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
- **A2 健康红线 `3452004`**、**F#5 结构日历两排 `b45fd46`**(先 `343ebd3` FlowRow→升级)——详见头部。
- **UI 清理批 `6a08ecf`**：#204/#190-P1 早已实现(SelectionSummaryBar 选择底部栏统一·状态曾滞后) · #190-P2 无食材保存软化(去打断对话框→浅 Snackbar) · #192 拍照删除撤销(ImagePickerButton onImageDeleted 上抛·封面+步骤图接·IngredientEditor Dialog 留 follow-up)。
- **#2 概览卡视觉打磨=跳过**：现状复用 §9.35 去红色阶(`nutritionLevelColor` 琥珀→绿)+主题自适应文字·已达标·无边际价值。
- 门禁贯彻：每功能 Explore 摸现状→(交互/视觉/质量)门禁→构建(daemon 偶瞬崩·重试即过·非编译错)→单测(改动纯 androidApp UI 层·shared 未触)→commit→push。**本轮所有 UI 相关可实现项已清空**(见下四)。

## 三、上一 session 交付（17 提交·全 push·末位 `4cf9ddb`）
- **确定方案 D 批**：D3 膳食均衡度色去红(收敛 nutritionLevelColor) · D4 周计划慢病 GI/嘌呤软降(抽 ChronicDiseasePenalty 与单餐同口径) · D2 AiSettings 卡化 · D1-1 CookMode + D1-2 FamilyEdit **VM 化(F-Arch2/3 结案·min-fix)**。
- **算法拍板批**：B#6 recommend 封顶 · C#F1 强版模型输出后补组合缺口 · C#F2 早餐软硬透传单餐(避免白粥+豆浆两软无蛋)。均单测+Google 审。
- **🌟周计划营养线全链路**：domain 一期(`WeeklyNutritionLineAggregator`+`NutritionLineAdvisor`+8 单测·结构层) + **AiPlan「一周营养搭配」概览卡**(把整周结构覆盖/缺口/均衡度盛出来·去红·免责)。
- **🌟AiPlan 界面统一 P1**：DayCard 白卡化 + 控件折叠进「计划设置」弹层 + 提示条独立 + 空态 EmptyState + 标注去 emoji(§9.35)。
- **🌟推荐带营养素热量**：AiRecommend 每菜"整份约 X 千卡·蛋白/脂肪/碳水"行 + 组合卡合计 + 钠"偏咸"提示 + 缺数据"待完善"·热量受开关(§9.36)·规则+模型+兜底三路统一·`DishNutritionLine`/`rememberCalorieNumberEnabled`。
- **拍照**：删除入口迁全屏查看器(去缩略图角标免误触·单图铺开·§9.34) + **查看器删当前图预览不刷新 bug 修**(produceState key preview 恒 null·用户真机抓的)。
- **报告空周/月→跳一周计划**(带周日期·月含月首日·`WEEK_PLAN_ROUTE` 参数化)。
- 会商方案：营养线四角色(产品/营养师/运营/文案) · AiPlan界面统一 · A#4/A#6 得失分析。

## 四、⏭ 下一步（**UI 相关可实现项本轮全清空**·用户 2026-07-21 收尾交接·剩余各有约束）
> ✅ 本续接 session 已做完：营养线三件套(#1/#3/#4) · A2 孕哺不评热量 · F#5 结构日历两排 · UI 清理批(#204/#190/#192) · backlog 对账。#2 判跳过。
>
> **① 数据扩充 + 全数据源脚本交叉核对 + 资产化/cron = ✅ 本 session 全部完成**(见头部①②③ + `数据扩充_营养核准_待跑.md`/`全数据源交叉核对_发现.md`/`数据获取脚本化方案.md`)。**数据线告一段落**，剩：
> - 🟡 **4项待核交用户把关**(玫瑰花/章鱼/年糕/生蚝·kcal或蛋白存疑·未擅改)——用户确认后修，或后续一手权威核。
> - (可选自主)nlc匹配迭代提命中·让cron月度体检更全·非紧急。
> - 第三批数据(剩余gi/satFat/purine缺口)**ROI递减**(多为USDA无对应中国特有食材/无标准GI调料酒),按需非批量。
>
> **② 剩余 UI（都非"干净可无人值守"·建议用户在场/定向）**：
> - **#203 食材编辑器 Dialog→路由页**：唯一实质剩余 UI 重构·**高爆炸半径**(多处以 Dialog 打开·转路由要改导航+所有打开点+传参)·建议用户在场做。视觉家族化(InsetGroup/AppTopBar/FoldSection)其实已做,只差载体转换(转了顺带解 #192 IngredientEditor 撤销宿主受限)。
> - **#208 更新基础数据启动弹窗(透明准则)**：后端 changelog+更新记录中心已做·启动弹窗"v1基线不触发·待下次数据变更才有效"→**现在做真机也验不了**·待有数据更新(如数据扩充落地)时一起做。
> - **#177 运营一期首屏化**：需运营设计门禁+方向定义·非纯实现。
>
> **③ 算法/健康(需用户拍板)**：A#6-B 当日能量暗因子(不可验证+与"热量个人概念"红线张力·未显式拍板·建议先确认) · A#4 GI(低价值可不做) · CookingTimer VM化(需真机验计时) · #209/#210 健康忌口补漏(联网核准·用户历来亲自把关·勿盲改)。

> **接手推进**：读 `待办总览`(唯一 backlog 真相源) + `全数据源交叉核对_发现.md` + 本档 → **UI + 数据扩充 + 数据交叉核对+资产化/cron 均已收尾**。当前所有"可无人值守安全推的主力"已清空，剩余项**均需用户介入**：4项待核(把关)、#203重构(在场·高爆炸半径)、A#6-B暗因子(拍板)、#209/#210健康忌口补漏(用户亲自核)、#208更新弹窗(待数据更新)、#177运营(方向定义)。**建议用户醒后先处理4项数据待核**(最快·飞书已列)，再定下一步方向。每功能过门禁+构建+push·真机验证用户统一做。
