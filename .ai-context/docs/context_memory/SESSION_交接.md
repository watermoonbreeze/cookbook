# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + 同目录 `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-22 · 数据/编辑 session（剂量默认值专项 P1/P2/P4 + A1空量修复 + A2/C查证，全部已 push：`417e0a9`/`95acf72`+交接docs）。本数据 session 已收尾清空·剩 off-type 各单开 session。**

## 本 session 交付（数据/编辑类·深度·无人值守）
- ✅ **P1 修加食材单位丢失 bug**（`NewDishViewModel`）：`gramUnit()` 依赖异步 `availableUnits`、被"菜名自动加食材"竞速→units 未加载返 null→默认100g 落到计件默认单位(个/只)/NULL→详情"青椒100.0个/肉丝100.0无单位"、营养按错单位折算。修=`unitsReady:CompletableDeferred`+`cachedGramUnit`+`autoAddFromName` await 单位就绪+加食材 `unitId=gram?.id`(移除落 `defaultUnitId` 的错兜底=根因)+units 加载 runCatching 防永挂+餐次预选前置解耦。
- ✅ **P2 智能默认克数**（`SeasoningDefaults`+单测）：非调料按 `FoodGroup.classify` 给经验默认(蛋50/菜150/奶200/肉150…惯例非权威可改)·判不出退100。
- ✅ **P4 修复历史错单位数据**（`Cookbook.sq`+`PresetDataSeeder`）：幂等 `repairDishIngredientGramUnitForDefault`(quantity=100 且 unit 空/计件→gram)·`SEED_LOGIC_VERSION` v9→v10·**纯UPDATE无.sqm迁移**·真机库副本验证(精确修10行/144合法行不动/幂等残留0)。
- ✅ **透明**：`changelog.json` v2 告知用户(修单位显示+分类默认克数)。
- ✅ **A1 修 quantity=NULL 配料(0营养)**：seeder 幂等修 user 自建菜空用量配料(8行)→按食材名分类补默认克数+克单位(`selectUserDishIngredientsWithNullQuantity` 仅 source='user'·`SEED_LOGIC_VERSION` v11)·加 `PresetDataSeederTest` 回归·真机库副本验证。
- ✅ **A2/C 查证关待办**：A2=`dish_ingredient(dish_id,ingredient_id)` 确认有 `uq_dish_ingredient` UNIQUE；C=RANDOM IN 999崩溃隐患**早已修**(`listAllDishMinis` 无 IN)→均标结论关闭·零代码(先查证再改·不虚做)。
- **门禁**：剂量三件套过 google_quality_engineer 无阻断(2建议已收口)+独立 /code-review 无新bug；A1 小改自评(复用已测原语+回归测试)。构建 `:shared:testDebugUnitTest`+`:androidApp:assembleDebug` 均绿。真机验待用户做。
- 决策日志：`context_memory/unattended_decisions.md`(2026-07-22 条)。

## ⏭ 下一步（本数据 session 主线已清·以下均 off-type·各单开 session）
- 【**UI**·本 session 新登记】①**添加食材 vs 添加菜品两编辑页不统一**(字体色/名称范式:一个外置label一个内部placeholder·抽共享 `FormField`/`FormSection` 根治反复·待办🔴)②**营养走势折线三线同显**(蛋白/脂肪/碳水免切换)+**周/月视图统一评估**("整体调研"·待办🟡)。两项**须单开 UI session + Apple-UX 门禁**。
- 【**会商**】「是否吃完/实际食用量影响摄入」多方会商(待办 A#📄·关联北极星营养统计准确性)。
- 【**数据/健康**·需你 greenlight+联网核准】四项数据待核(玫瑰花/章鱼/年糕/生蚝)、全库忌口补漏剩余边界(鸡毛蛋/鱼油/植物高嘌呤)、GI/纤维覆盖补齐——健康数据你历来亲自把关。

## 先读清单
1. 本文件 + `待办总览.md`（本 session 完成=#30/#31 剂量专项 + 自建菜空量 A1 + A2/C 查证关闭；新登记=[UI统一]两编辑页、[UI/调研]营养走势三线两行；off-type 队列=是否吃完会商、健康数据需 greenlight 项）。
2. `unattended_decisions.md`(2026-07-22 剂量专项决策+取舍)。
3. `CLAUDE.md` 踩坑红线(尤其 SQLDelight/加列/seed 指纹/单位英文化)+架构准则+ `~/.claude/workflow_auto_orchestration.md`(编码流程:级别区间+第六章能力层门禁)。

## 工作规则（用户已定·稳定）
1. 🔴 **一个 session 只做一类**·off-type 进待办不当场做·切类型=换 session。
2. 中文·深度·无人值守·全权推进·快速做完不反复问确认。每功能过门禁(界面→apple_ux_designer / App自动行为→apple_software_behavior / 文案→copywriter / Android代码→google_quality_engineer·标准/深度叠加 /code-review)+构建+单测。
3. 🔴 构建看输出别信 exit code（grep `BUILD SUCCESSFUL`）。数据 bug 先 python 拉真机库(`adb pull` 外部路径·`MSYS_NO_PATHCONV=1`·列名 energy_kcal)证实再改。**temp/ 未 gitignore→提交必显式 add 指定文件、绝不 `git add -A`**。
4. 每功能一批→门禁→构建+单测→commit(`[unattended]`)→**push origin/master**→落档。真机验证用户统一做。
5. 红线：健康免责·透明分级告知·联网核准列数据来源·AI生成内容落库·热量个人概念(受 `CALORIE_NUMBER_ENABLED`·不折算人均)·抽共享防调参漂移·SQLDelight 改表必加 `.sqm`(纯查询/UPDATE 免迁移)。
